#ifndef XBYTES_LINUX_UTILS_H
#define XBYTES_LINUX_UTILS_H
#include <cstdint>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdexcept>

std::uint8_t *memory_map(int fd, std::size_t offset, bool readOnly, std::size_t length, bool preTouch)
{
    int flags = MAP_SHARED;

    if (preTouch)
    {
        flags = flags | MAP_POPULATE;
    }

    void *memory = ::mmap(

        nullptr,
        length,
        readOnly ? PROT_READ : (PROT_READ | PROT_WRITE),
        flags,
        fd,
        static_cast<off_t>(offset));

    if (MAP_FAILED == memory)
    {
        throw std::runtime_error("failed to Memory Map file");
    }

    return static_cast<std::uint8_t *>(memory);
}

#endif