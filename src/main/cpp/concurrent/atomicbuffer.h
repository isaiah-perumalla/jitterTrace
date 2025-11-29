#include <cstdint>
#include <cstring>
#include <cassert>
#include <atomic>

#ifndef xBytes_CONCURRENT_ATOMIC_BUFFER_H
#define xBytes_CONCURRENT_ATOMIC_BUFFER_H

namespace xBytes
{
    namespace concurrent
    {

        /**
         * Atomic View over a block of bytes
         * Does not take owner ship of the *buffer
         */
        struct AtomicBytesView
        {
            AtomicBytesView(std::uint8_t *buffer, std::size_t length) : buffer{buffer},
                                                                        length{static_cast<uint32_t>(length)}
            {
                memset(buffer, static_cast<std::size_t>(length), 0);

            }

        private:
            void* buffer;
            std::uint32_t length;
        
            

        public:
            void set_uint64(uint32_t index, uint64_t val, std::memory_order ordering) {
                //check alignment
                std::atomic<uint64_t> *dest = get_atomic(index);
                dest->store(val, ordering);
            }

            std::atomic<uint64_t>* get_atomic(uint32_t index)
            {
                void *ptr = ((char*)buffer + index);
                static_assert(alignof(std::atomic<uint64_t>) == alignof(uint64_t));
                assert(alignof(std::atomic<uint64_t>) == alignof(ptr));
                std::atomic<uint64_t> *dest = static_cast<std::atomic<uint64_t> *>(ptr);
                return dest;
            }
            
            uint64_t get_uint64(uint32_t index, std::memory_order ordering) {
                std::atomic<uint64_t>* at = get_atomic(index);
                return at->load(ordering);
            }

        };

    }
}

#endif