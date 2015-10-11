#include <stdint.h>

class DataPacked{
public:
    uint32_t frame_id_;
    uint32_t color_size_;
    char     data_[10000];
}__attribute__((packed));


