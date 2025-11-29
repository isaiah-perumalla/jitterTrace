#include <unistd.h>
#include "linuxutils.h"
#include "atomicbuffer.h"
#include <vector>
#include <thread>


void count(std::atomic<uint64_t>* atomic_int, uint32_t limit) {
    for(int i = 0; i < limit; i++) {
        // const uint64_t x = atomic_int->load(std::memory_order::memory_order_relaxed);
        // atomic_int->store(x+1, std::memory_order::memory_order_relaxed);
        
        atomic_int->fetch_add(1, std::memory_order::memory_order_relaxed);
    }
    printf("thread %d completed\n", std::this_thread::get_id());
}

int main(int argc, char *argv[]) {

    if (argc != 2) {
        printf("Usage: %s <file>\n", argv[0]);
        exit(1);
    }

    const int fd = open(argv[1], O_CREAT | O_RDWR, S_IRUSR | S_IWUSR | S_IROTH);
    if (fd < 0) {
        throw std::runtime_error("failed to open file");
    }
    ftruncate64(fd, 128);

    uint8_t* memory = memory_map(fd, 0, false, 128, true);
    xBytes::concurrent::AtomicBytesView atomic_buffer {memory, 128};

    atomic_buffer.set_uint64(0, 0, std::memory_order_relaxed);
    std::atomic_thread_fence(std::memory_order_release);
    std::atomic<uint64_t>* atomic_int = atomic_buffer.get_atomic(0);

    
    const uint32_t n = 5;
    const uint32_t count_per_thread = 1000000;
    
    
    std::vector<std::thread> threads(n);
    // spawn n threads:
    
    for (int i = 0; i < n; i++) {
        threads[i] = std::thread( [&]{count(atomic_int, count_per_thread);});
    }

    for (auto& th : threads) {
        th.join();
    }
    const u_int64_t i = atomic_buffer.get_uint64(0, std::memory_order::memory_order_acquire);
    printf("acutal total count=%d, expected=%d \n", i, n * count_per_thread);

    return 0;

}