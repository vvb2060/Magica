/*
 * Copyright (c) 1997-8,2008,20 Andrew G. Morgan <morgan@kernel.org>
 *
 * This file deals with flipping of capabilities on internal
 * capability sets as specified by POSIX.1e (formerlly, POSIX 6).
 *
 * It also contains similar code for bit flipping cap_iab_t values.
 */

#include "libcap.h"

/*
 * Return the state of a specified capability flag.  The state is
 * returned as the contents of *raised.  The capability is from one of
 * the sets stored in cap_d as specified by set and value
 */

int cap_get_flag(cap_t cap_d, cap_value_t value, cap_flag_t set,
		 cap_flag_value_t *raised)
{
    /*
     * Do we have a set and a place to store its value?
     * Is it a known capability?
     */

    if (raised && good_cap_t(cap_d) && value >= 0 && value < __CAP_MAXBITS
	&& set >= 0 && set < NUMBER_OF_CAP_SETS) {
	*raised = isset_cap(cap_d,value,set) ? CAP_SET:CAP_CLEAR;
	return 0;
    } else {
	_cap_debug("invalid arguments");
	errno = EINVAL;
	return -1;
    }
}

/*
 * raise/lower a selection of capabilities
 */

int cap_set_flag(cap_t cap_d, cap_flag_t set,
		 int no_values, const cap_value_t *array_values,
		 cap_flag_value_t raise)
{
    /*
     * Do we have a set and a place to store its value?
     * Is it a known capability?
     */

    if (good_cap_t(cap_d) && no_values > 0 && no_values < __CAP_MAXBITS
	&& (set >= 0) && (set < NUMBER_OF_CAP_SETS)
	&& (raise == CAP_SET || raise == CAP_CLEAR) ) {
	int i;
	for (i=0; i<no_values; ++i) {
	    if (array_values[i] < 0 || array_values[i] >= __CAP_MAXBITS) {
		_cap_debug("weird capability (%d) - skipped", array_values[i]);
	    } else {
		int value = array_values[i];

		if (raise == CAP_SET) {
		    cap_d->raise_cap(value,set);
		} else {
		    cap_d->lower_cap(value,set);
		}
	    }
	}
	return 0;

    } else {

	_cap_debug("invalid arguments");
	errno = EINVAL;
	return -1;

    }
}

/*
 *  Reset the capability to be empty (nothing raised)
 */

int cap_clear(cap_t cap_d)
{
    if (good_cap_t(cap_d)) {
	memset(&(cap_d->u), 0, sizeof(cap_d->u));
	return 0;
    } else {
	_cap_debug("invalid pointer");
	errno = EINVAL;
	return -1;
    }
}

/*
 *  Reset the all of the capability bits for one of the flag sets
 */

int cap_clear_flag(cap_t cap_d, cap_flag_t flag)
{
    switch (flag) {
    case CAP_EFFECTIVE:
    case CAP_PERMITTED:
    case CAP_INHERITABLE:
	if (good_cap_t(cap_d)) {
	    unsigned i;

	    for (i=0; i<_LIBCAP_CAPABILITY_U32S; i++) {
		cap_d->u[i].flat[flag] = 0;
	    }
	    return 0;
	}
	/*
	 * fall through
	 */

    default:
	_cap_debug("invalid pointer");
	errno = EINVAL;
	return -1;
    }
}

/*
 * Compare two capability sets
 */
int cap_compare(cap_t a, cap_t b)
{
    unsigned i;
    int result;

    if (!(good_cap_t(a) && good_cap_t(b))) {
	_cap_debug("invalid arguments");
	errno = EINVAL;
	return -1;
    }

    for (i=0, result=0; i<_LIBCAP_CAPABILITY_U32S; i++) {
	result |=
	    ((a->u[i].flat[CAP_EFFECTIVE] != b->u[i].flat[CAP_EFFECTIVE])
	     ? LIBCAP_EFF : 0)
	    | ((a->u[i].flat[CAP_INHERITABLE] != b->u[i].flat[CAP_INHERITABLE])
	       ? LIBCAP_INH : 0)
	    | ((a->u[i].flat[CAP_PERMITTED] != b->u[i].flat[CAP_PERMITTED])
	       ? LIBCAP_PER : 0);
    }
    return result;
}

/*
 * cap_iab_get_vector reads the single bit value from an IAB vector set.
 */
cap_flag_value_t cap_iab_get_vector(cap_iab_t iab, cap_iab_vector_t vec,
				    cap_value_t bit)
{
    if (!good_cap_iab_t(iab) || bit >= cap_max_bits()) {
	return 0;
    }

    unsigned o = (bit >> 5);
    __u32 mask = 1u << (bit & 31);

    switch (vec) {
    case CAP_IAB_INH:
	return !!(iab->i[o] & mask);
	break;
    case CAP_IAB_AMB:
	return !!(iab->a[o] & mask);
	break;
    case CAP_IAB_BOUND:
	return !!(iab->nb[o] & mask);
	break;
    default:
	return 0;
    }
}

/*
 * cap_iab_set_vector sets the bits in an IAB to the value
 * raised. Note, setting A implies setting I too, lowering I implies
 * lowering A too.  The B bits are, however, independently settable.
 */
int cap_iab_set_vector(cap_iab_t iab, cap_iab_vector_t vec, cap_value_t bit,
		       cap_flag_value_t raised)
{
    if (!good_cap_iab_t(iab) || (raised >> 1) || bit >= cap_max_bits()) {
	errno = EINVAL;
	return -1;
    }

    unsigned o = (bit >> 5);
    __u32 on = 1u << (bit & 31);
    __u32 mask = ~on;

    switch (vec) {
    case CAP_IAB_INH:
	iab->i[o] = (iab->i[o] & mask) | (raised ? on : 0);
	iab->a[o] &= iab->i[o];
	break;
    case CAP_IAB_AMB:
	iab->a[o] = (iab->a[o] & mask) | (raised ? on : 0);
	iab->i[o] |= iab->a[o];
	break;
    case CAP_IAB_BOUND:
	iab->nb[o] = (iab->nb[o] & mask) | (raised ? on : 0);
	break;
    default:
	errno = EINVAL;
	return -1;
    }

    return 0;
}

/*
 * cap_iab_fill copies a bit-vector of capability state from a cap_t
 * to a cap_iab_t. Note, because the bounding bits in an iab are to be
 * dropped when applied, the copying process, when to a CAP_IAB_BOUND
 * vector involves inverting the bits. Also, adjusting I will mask
 * bits in A, and adjusting A may implicitly raise bits in I.
 */
int cap_iab_fill(cap_iab_t iab, cap_iab_vector_t vec,
		 cap_t cap_d, cap_flag_t flag)
{
    if (!good_cap_t(cap_d) || !good_cap_iab_t(iab)) {
	errno = EINVAL;
	return -1;
    }

    switch (flag) {
    case CAP_EFFECTIVE:
    case CAP_INHERITABLE:
    case CAP_PERMITTED:
	break;
    default:
	errno = EINVAL;
	return -1;
    }

    int i;
    for (i = 0; i < _LIBCAP_CAPABILITY_U32S; i++) {
	switch (vec) {
	case CAP_IAB_INH:
	    iab->i[i] = cap_d->u[i].flat[flag];
	    iab->a[i] &= iab->i[i];
	    break;
	case CAP_IAB_AMB:
	    iab->a[i] = cap_d->u[i].flat[flag];
	    iab->i[i] |= cap_d->u[i].flat[flag];
	    break;
	case CAP_IAB_BOUND:
	    iab->nb[i] = ~cap_d->u[i].flat[flag];
	    break;
	default:
	    errno = EINVAL;
	    return -1;
	}
    }

    return 0;
}
