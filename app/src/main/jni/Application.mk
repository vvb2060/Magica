APP_STL        := none
APP_LDFLAGS    := -Wl,-exclude-libs,ALL -Wl,--gc-sections -s
APP_CFLAGS     := -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections -Wl,-exclude-libs,ALL -Wl,--gc-sections
APP_STRIP_MODE := --strip-all --remove-section=.comment
