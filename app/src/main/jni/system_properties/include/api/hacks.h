#pragma once

#undef __INTRODUCED_IN
#define __INTRODUCED_IN(...)

#undef __BIONIC_AVAILABILITY_GUARD
#define __BIONIC_AVAILABILITY_GUARD(...) 1

// <async_safe/CHECK.h>
#define CHECK(x)  /* NOP */

// <async_safe/log.h>
#define async_safe_format_buffer snprintf
#define async_safe_format_log(...)  /* NOP */

// Rename symbols
#pragma redefine_extname __system_property_set __system_property_set2
#pragma redefine_extname __system_property_find __system_property_find2
#pragma redefine_extname __system_property_read_callback __system_property_read_callback2
#pragma redefine_extname __system_property_foreach __system_property_foreach2
#pragma redefine_extname __system_property_wait __system_property_wait2
#pragma redefine_extname __system_property_read __system_property_read2
#pragma redefine_extname __system_property_get __system_property_get2
#pragma redefine_extname __system_property_find_nth __system_property_find_nth2
#pragma redefine_extname __system_property_set_filename __system_property_set_filename2
#pragma redefine_extname __system_property_area_init __system_property_area_init2
#pragma redefine_extname __system_property_area_serial __system_property_area_serial2
#pragma redefine_extname __system_property_add __system_property_add2
#pragma redefine_extname __system_property_update __system_property_update2
#pragma redefine_extname __system_property_serial __system_property_serial2
#pragma redefine_extname __system_properties_init __system_properties_init2
#pragma redefine_extname __system_property_wait_any __system_property_wait_any2
