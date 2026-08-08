package com.smartchat.ai;

import java.io.IOException;

/**
 * AI 客户端接口：屏蔽不同模型厂商（OpenAI 兼容协议 / Anthropic Claude）的差异。
 * <p>
 * 实现类需要支持「阻塞式流式调用」：
 * 增量文本通过 onDelta 回调，结束时通过 onUsage 回调用量；
 * 调用期间通过 {@link AiStreamHandle} 感知取消（用户点了「停止生成」）。
 */
public interface AiClient {

    /** 厂商标识：openai / anthropic */
    String provider();

    /**
     * 流式生成。
     *
     * @param request 请求（已翻译成厂商无关格式）
     * @param handle  流句柄（用于取消）
     * @param onDelta 文本增量回调
     * @param onUsage 用量回调（流结束时调用一次，可为空）
     * @throws IOException 上游网络/协议错误
     */
    void stream(AiRequest request, AiStreamHandle handle,
                java.util.function.Consumer<String> onDelta,
                java.util.function.Consumer<AiRequest.AiUsage> onUsage) throws IOException;

    /** 流句柄：客户端断开或用户点「停止」时置为已取消 */
    final class AiStreamHandle {
        private volatile boolean cancelled = false;

        public void cancel() {
            cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
