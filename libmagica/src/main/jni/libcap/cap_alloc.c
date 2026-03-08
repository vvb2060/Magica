/*
 * Copyright (c) 1997-8,2019 Andrew G Morgan <morgan@kernel.org>
 *
 * This file deals with allocation and deallocation of internal
 * capability sets as specified by POSIX.1e (formerlly, POSIX 6).
 */

#include "libcap.h"

/*
 * This gets set via the pre-main() executed constructor function below it.
 */
static cap_value_t _cap_max_bits;

__attribute__((constructor (300))) static void _initialize_libcap(void) {
    if (_cap_max_bits) {
	return;
    }
    cap_set_syscall(NULL, NULL);
    _binary_search(_cap_max_bits, cap_get_bound, 0, __CAP_MAXBITS, __CAP_BITS);
}

cap_value_t cap_max_bits(void) {
    return _cap_max_bits;
}

/*
 * Obtain a blank set of capabilities
 */

cap_t cap_init(void)
{
    __u32 *raw_data;
    cap_t result;

    raw_data = calloc(1, sizeof(__u32) + sizeof(*result));
    if (raw_data == NULL) {
	_cap_debug("out of memory");
	errno = ENOMEM;
	return NULL;
    }

    *raw_data = CAP_T_MAGIC;
    result = (cap_t) (raw_data + 1);

    result->head.version = _LIBCAP_CAPABILITY_VERSION;
    capget(&result->head, NULL);      /* load the kernel-capability version */

    switch (result->head.version) {
#ifdef _LINUX_CAPABILITY_VERSION_1
    case _LINUX_CAPABILITY_VERSION_1:
	break;
#endif
#ifdef _LINUX_CAPABILITY_VERSION_2
    case _LINUX_CAPABILITY_VERSION_2:
	break;
#endif
#ifdef _LINUX_CAPABILITY_VERSION_3
    case _LINUX_CAPABILITY_VERSION_3:
	break;
#endif
    default:                          /* No idea what to do */
	cap_free(result);
	result = NULL;
	break;
    }

    return result;
}

/*
 * This is an internal library function to duplicate a string and
 * tag the result as something cap_free can handle.
 */

char *_libcap_strdup(const char *old)
{
    __u32 *raw_data;

    if (old == NULL) {
	errno = EINVAL;
	return NULL;
    }

    raw_data = malloc( sizeof(__u32) + strlen(old) + 1 );
    if (raw_data == NULL) {
	errno = ENOMEM;
	return NULL;
    }

    *(raw_data++) = CAP_S_MAGIC;
    strcpy((char *) raw_data, old);

    return ((char *) raw_data);
}

/*
 * This function duplicates an internal capability set with
 * malloc()'d memory. It is the responsibility of the user to call
 * cap_free() to liberate it.
 */

cap_t cap_dup(cap_t cap_d)
{
    cap_t result;

    if (!good_cap_t(cap_d)) {
	_cap_debug("bad argument");
	errno = EINVAL;
	return NULL;
    }

    result = cap_init();
    if (result == NULL) {
	_cap_debug("out of memory");
	return NULL;
    }

    memcpy(result, cap_d, sizeof(*cap_d));

    return result;
}

cap_iab_t cap_iab_init(void) {
    __u32 *base = calloc(1, sizeof(__u32) + sizeof(struct cap_iab_s));
    *(base++) = CAP_IAB_MAGIC;
    return (cap_iab_t) base;
}

/*
 * cap_new_launcher allocates some memory for a launcher and
 * initializes it.  To actually launch a program with this launcher,
 * use cap_launch(). By default, the launcher is a no-op from a
 * security perspective and will act just as fork()/execve()
 * would. Use cap_launcher_setuid() etc to override this.
 */
cap_launch_t cap_new_launcher(const char *arg0, const char * const *argv,
			      const char * const *envp)
{
    __u32 *data = calloc(1, sizeof(__u32) + sizeof(struct cap_launch_s));
    *(data++) = CAP_LAUNCH_MAGIC;
    struct cap_launch_s *attr = (struct cap_launch_s *) data;
    attr->arg0 = arg0;
    attr->argv = argv;
    attr->envp = envp;
    return attr;
}

/*
 * Scrub and then liberate an internal capability set.
 */

int cap_free(void *data_p)
{
    if (!data_p)
	return 0;

    if (good_cap_t(data_p)) {
	data_p = -1 + (__u32 *) data_p;
	memset(data_p, 0, sizeof(__u32) + sizeof(struct _cap_struct));
	free(data_p);
	data_p = NULL;
	return 0;
    }

    if (good_cap_string(data_p)) {
	size_t length = strlen(data_p) + sizeof(__u32);
     	data_p = -1 + (__u32 *) data_p;
     	memset(data_p, 0, length);
     	free(data_p);
     	data_p = NULL;
     	return 0;
    }

    if (good_cap_iab_t(data_p)) {
	size_t length = sizeof(struct cap_iab_s) + sizeof(__u32);
	data_p = -1 + (__u32 *) data_p;
	memset(data_p, 0, length);
	free(data_p);
	data_p = NULL;
	return 0;
    }

    if (good_cap_launch_t(data_p)) {
	cap_launch_t launcher = data_p;
	if (launcher->iab) {
	    cap_free(launcher->iab);
	}
	if (launcher->chroot) {
	    cap_free(launcher->chroot);
	}
	size_t length = sizeof(struct cap_iab_s) + sizeof(__u32);
	data_p = -1 + (__u32 *) data_p;
	memset(data_p, 0, length);
	free(data_p);
	data_p = NULL;
	return 0;
    }

    _cap_debug("don't recognize what we're supposed to liberate");
    errno = EINVAL;
    return -1;
}
