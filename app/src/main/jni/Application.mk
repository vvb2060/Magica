APP_STL     := none
APP_LDFLAGS := -Wl,-exclude-libs,ALL -Wl,--gc-sections -Wl,--strip-all
APP_CFLAGS  := -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections
APP_CFLAGS  += -Oz
