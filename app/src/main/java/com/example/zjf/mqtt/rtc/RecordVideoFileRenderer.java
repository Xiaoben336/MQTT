package com.example.zjf.mqtt.rtc;
import android.os.Handler;
import android.os.HandlerThread;

import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.RendererCommon;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoRenderer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.ArrayList;

public class RecordVideoFileRenderer implements VideoRenderer.Callbacks{
    private static final String TAG = "VideoFileRenderer";

    private final HandlerThread renderThread;
    private final Object handlerLock = new Object();
    private final Handler renderThreadHandler;
    private final FileOutputStream videoOutFile;
    private final String outputFileName;
    private final int outputFileWidth;
    private final int outputFileHeight;
    private final int outputFrameSize;
    private final ByteBuffer outputFrameBuffer;
    private EglBase eglBase;
    private YuvConverter yuvConverter;
    private ArrayList<ByteBuffer> rawFrames = new ArrayList<>();

    public RecordVideoFileRenderer(String outputFile, int outputFileWidth, int outputFileHeight,
                                   final EglBase.Context sharedContext) throws IOException {
        if ((outputFileWidth % 2) == 1 || (outputFileHeight % 2) == 1) {
            throw new IllegalArgumentException("Does not support uneven width or height");
        }

        this.outputFileName = outputFile;
        this.outputFileWidth = outputFileWidth;
        this.outputFileHeight = outputFileHeight;

        outputFrameSize = outputFileWidth * outputFileHeight * 3 / 2;
        outputFrameBuffer = ByteBuffer.allocateDirect(outputFrameSize);

        videoOutFile = new FileOutputStream(outputFile);
        videoOutFile.write(
                ("YUV4MPEG2 C420 W" + outputFileWidth + " H" + outputFileHeight + " Ip F30:1 A1:1\n")
                        .getBytes());

        renderThread = new HandlerThread(TAG);
        renderThread.start();
        renderThreadHandler = new Handler(renderThread.getLooper());

        ThreadUtils.invokeAtFrontUninterruptibly(renderThreadHandler, new Runnable() {
            @Override
            public void run() {
                eglBase = EglBase.create(sharedContext, EglBase.CONFIG_PIXEL_BUFFER);
                eglBase.createDummyPbufferSurface();
                eglBase.makeCurrent();
                yuvConverter = new YuvConverter();
            }
        });
    }

    @Override
    public void renderFrame(final VideoRenderer.I420Frame frame) {
        renderThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                renderFrameOnRenderThread(frame);
            }
        });
    }


    private void renderFrameOnRenderThread(VideoRenderer.I420Frame frame) {
        final float frameAspectRatio = (float) frame.rotatedWidth() / (float) frame.rotatedHeight();

        final float[] rotatedSamplingMatrix =
                RendererCommon.rotateTextureMatrix(frame.samplingMatrix, frame.rotationDegree);
        final float[] layoutMatrix = RendererCommon.getLayoutMatrix(
                false, frameAspectRatio, (float) outputFileWidth / outputFileHeight);
        final float[] texMatrix = RendererCommon.multiplyMatrices(rotatedSamplingMatrix, layoutMatrix);

        try {
            ByteBuffer buffer = VideoFileRenderer.nativeCreateNativeByteBuffer(outputFrameSize);
            if (!frame.yuvFrame) {
                yuvConverter.convert(outputFrameBuffer, outputFileWidth, outputFileHeight, outputFileWidth,
                        frame.textureId, texMatrix);

                int stride = outputFileWidth;
                byte[] data = outputFrameBuffer.array();
                int offset = outputFrameBuffer.arrayOffset();

                // Write Y
                buffer.put(data, offset, outputFileWidth * outputFileHeight);

                // Write U
                for (int r = outputFileHeight; r < outputFileHeight * 3 / 2; ++r) {
                    buffer.put(data, offset + r * stride, stride / 2);
                }

                // Write V
                for (int r = outputFileHeight; r < outputFileHeight * 3 / 2; ++r) {
                    buffer.put(data, offset + r * stride + stride / 2, stride / 2);
                }
            } else {
                VideoFileRenderer.nativeI420Scale(frame.yuvPlanes[0], frame.yuvStrides[0], frame.yuvPlanes[1],
                        frame.yuvStrides[1], frame.yuvPlanes[2], frame.yuvStrides[2], frame.width, frame.height,
                        outputFrameBuffer, outputFileWidth, outputFileHeight);

                buffer.put(outputFrameBuffer.array(), outputFrameBuffer.arrayOffset(), outputFrameSize);
            }
            buffer.rewind();
            rawFrames.add(buffer);
        } finally {
            VideoRenderer.renderFrameDone(frame);
        }
    }

    /**
     * Release all resources. All already posted frames will be rendered first.
     */
    public void release() {
        final CountDownLatch cleanupBarrier = new CountDownLatch(1);
        renderThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                yuvConverter.release();
                eglBase.release();
                renderThread.quit();
                cleanupBarrier.countDown();
            }
        });
        ThreadUtils.awaitUninterruptibly(cleanupBarrier);
        try {
            for (ByteBuffer buffer : rawFrames) {
                videoOutFile.write("FRAME\n".getBytes());

                byte[] data = new byte[outputFrameSize];
                buffer.get(data);

                videoOutFile.write(data);

                VideoFileRenderer.nativeFreeNativeByteBuffer(buffer);
            }
            videoOutFile.close();
            Logging.d(TAG, "Video written to disk as " + outputFileName + ". Number frames are "
                    + rawFrames.size() + " and the dimension of the frames are " + outputFileWidth + "x"
                    + outputFileHeight + ".");
        } catch (IOException e) {
            Logging.e(TAG, "Error writing video to disk", e);
        }
    }
}
