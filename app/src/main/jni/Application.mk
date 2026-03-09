APP_LDFLAGS    := -Wl,-exclude-libs,ALL -Wl,--gc-sections -Wl,--strip-all -Wl,--icf=all -flto
APP_CFLAGS     := -Wall -Wextra -Werror -fvisibility=hidden -fvisibility-inlines-hidden
APP_CFLAGS     += -flto
APP_CONLYFLAGS := -std=c23
APP_CPPFLAGS   := -std=c++23
APP_STL        := none
