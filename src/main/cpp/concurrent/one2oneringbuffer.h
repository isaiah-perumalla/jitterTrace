#ifndef XBYTES_RING_BUFFER_ONE_TO_ONE_H
#define XBYTES_RING_BUFFER_ONE_TO_ONE_H

#include <stdexcept>
#include <cstdint>
#include "atomicbuffer.h"

namespace xBytes
{
    namespace concurrent
    {
        static const int32_t TAIL_POSITION_OFFSET = CACHE_LINE_LENGTH * 2;
        static const int32_t HEAD_CACHE_POSITION_OFFSET = CACHE_LINE_LENGTH * 4;
        static const int32_t HEAD_POSITION_OFFSET = CACHE_LINE_LENGTH * 6;
        static const int32_t CORRELATION_COUNTER_OFFSET = CACHE_LINE_LENGTH * 8;
        static const int32_t CONSUMER_HEARTBEAT_OFFSET = CACHE_LINE_LENGTH * 10;
        static const int32_t TRAILER_LENGTH = CACHE_LINE_LENGTH * 12;

        static const int32_t HEADER_LENGTH = sizeof(std::int32_t) * 2;
        static const int32_t ALIGNMENT = HEADER_LENGTH;
        static const int32_t PADDING_MSG_TYPE_ID = -1;

        class One2OneRingBuffer
        {
        public:
            explicit One2OneRingBuffer(AtomicBytesView &buffer) : buffer{buffer}
            {
                capacity = buffer.capacity() - TRAILER_LENGTH;
            }

            One2OneRingBuffer(const One2OneRingBuffer &) = delete;
            One2OneRingBuffer &operator=(const One2OneRingBuffer &) = delete;

            int32_t tryClaim(int32_t msgTypeId, int32_t length)
            {
                if (msgTypeId < 1)
                {
                    throw std::runtime_error("invalid msgTypeId");
                }
                if (length < 0)
                {
                    throw std::runtime_error("invalid length " + length);
                }
                else if (length > maxMsgLength)
                {
                    throw std::runtime_error(
                        "encoded message exceeds maxMsgLength=" + maxMsgLength);
                }
                const int32_t record_length = length + HEADER_LENGTH;
                const int32_t record_index = claim_capacity(record_length);
            }

        private:
            AtomicBytesView &buffer;
            int32_t capacity;
            int32_t maxMsgLength;
            int32_t headPositionIndex;
            int32_t headCachePositionIndex;
            int32_t tailPositionIndex;
            int32_t correlationIdCounterIndex;
            int32_t consumerHeartbeatIndex;

            int32_t claim_capacity(int32_t length)
            {
                const int32_t alignedRecordLength = align(length);
                const int32_t required_capacity = alignedRecordLength + HEADER_LENGTH;
                const int32_t cap = capacity;
                const int32_t tailPositionIndex = tailPositionIndex;
                const int32_t headCachePositionIndex = headCachePositionIndex;
                const int32_t mask = capacity - 1;
            }

            inline static bool is_pow2(int32_t val) {
                return val > 0 && (val & (val -1)) == 0;
            }

            inline static int32_t align(int32_t val) {
                static_assert(is_pow2(ALIGNMENT));
                return val + (val & (ALIGNMENT-1));
            }
            
            // header is msgType length
            inline static std::int32_t recordLength(std::int64_t header)
            {
                return static_cast<std::int32_t>(header);
            }

            inline static std::int32_t messageTypeId(std::int64_t header)
            {
                return static_cast<std::int32_t>(header >> 32);
            }
        };
    }
}

#endif