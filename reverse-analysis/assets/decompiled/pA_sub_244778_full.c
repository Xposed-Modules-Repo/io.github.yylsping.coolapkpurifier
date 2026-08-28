__int64 __fastcall sub_244778(const char *a1, __int64 a2, __int64 a3, __int64 a4, __int64 a5)
{
  unsigned __int64 v10; // x0
  __int64 i; // x8
  size_t v12; // x0
  size_t v13; // x19
  char *v14; // x20
  __int64 v15; // x0
  __int64 v16; // x0
  int v17; // w23
  size_t v18; // x0
  size_t v19; // x24
  char *v20; // x25
  unsigned __int64 v21; // x21
  unsigned __int64 v22; // x26
  __int64 v23; // x0
  __int64 v24; // x22
  __int64 v25; // x8
  size_t v26; // x0
  size_t v27; // x24
  char *v28; // x25
  unsigned __int64 v29; // x26
  __int64 v30; // x0
  __int64 v32; // [xsp+268h] [xbp-88h] BYREF
  size_t v33; // [xsp+270h] [xbp-80h]
  char *v34; // [xsp+278h] [xbp-78h]
  _QWORD v35[4]; // [xsp+280h] [xbp-70h] BYREF

  v35[3] = *(_QWORD *)(_ReadStatusReg(TPIDR_EL0) + 40); /*0x2447bc*/
  pthread_rwlock_wrlock(&stru_4C2690); /*0x2447c0*/
  if ( (byte_4C26C8 & 1) != 0 ) /*0x2447cc*/
    return pthread_rwlock_unlock(&stru_4C2690); /*0x2447cc*/
  v10 = (*(__int64 (__fastcall **)(const char *))(qword_4C2298 + 152))(a1); /*0x2447e4*/
  if ( v10 >= 0x14 ) /*0x2447ec*/
  {
    strcpy((char *)v35, "xzwl}k|(v}ujmz(qttmoit("); /*0x244800*/
    for ( i = 0; i != 23; ++i ) /*0x244880*/
      *((_BYTE *)v35 + i) -= 8; /*0x2448a0*/
    v32 = 0; /*0x2448b4*/
    v33 = 0; /*0x2448b4*/
    v34 = 0; /*0x2448b8*/
    v12 = strlen((const char *)v35); /*0x2448bc*/
    v13 = v12; /*0x2448c0*/
    if ( v12 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x2448c8*/
      sub_1EA30(&v32); /*0x244b74*/
    if ( v12 >= 0x17 ) /*0x2448d0*/
    {
      v21 = (v12 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x244940*/
      v14 = (char *)sub_368454(v21); /*0x24494c*/
      v33 = v13; /*0x244954*/
      v34 = v14; /*0x244954*/
      v32 = v21 | 1; /*0x244958*/
    }
    else
    {
      v14 = (char *)&v32 + 1; /*0x2448dc*/
      LOBYTE(v32) = 2 * v12; /*0x2448e0*/
      if ( !v12 ) /*0x2448e4*/
      {
LABEL_15:
        v14[v13] = 0; /*0x24496c*/
        sub_2302FC(&v32); /*0x244974*/
        if ( (v32 & 1) != 0 ) /*0x24497c*/
          sub_3684BC(v34); /*0x244984*/
        return pthread_rwlock_unlock(&stru_4C2690); /*0x244988*/
      }
    }
    memcpy(v14, v35, v13); /*0x244968*/
    goto LABEL_15; /*0x244968*/
  }
  v15 = netht_ctx_get_instance_576(v10); /*0x2448ec*/
  v16 = sub_265114(v15, a2); /*0x2448f4*/
  v17 = netht_ctx_get_instance_576(v16); /*0x2448fc*/
  memset(v35, 0, 24); /*0x244904*/
  v18 = strlen(a1); /*0x24490c*/
  v19 = v18; /*0x244910*/
  if ( v18 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x244918*/
    sub_1EA30(v35); /*0x244b7c*/
  if ( v18 >= 0x17 ) /*0x244920*/
  {
    v22 = (v18 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x244990*/
    v20 = (char *)sub_368454(v22); /*0x24499c*/
    v35[1] = v19; /*0x2449a4*/
    v35[2] = v20; /*0x2449a4*/
    v35[0] = v22 | 1; /*0x2449a8*/
    goto LABEL_18; /*0x2449a8*/
  }
  v20 = (char *)v35 + 1; /*0x24492c*/
  LOBYTE(v35[0]) = 2 * v18; /*0x244930*/
  if ( v18 ) /*0x244934*/
LABEL_18:
    memcpy(v20, a1, v19); /*0x2449ac*/
  v20[v19] = 0; /*0x2449bc*/
  sub_1D77E0(&v32, v35); /*0x2449c8*/
  v23 = sub_2650E4(v17); /*0x2449d4*/
  if ( (v32 & 1) != 0 ) /*0x2449dc*/
    v23 = sub_3684BC(v34); /*0x2449e4*/
  if ( (v35[0] & 1) != 0 ) /*0x2449ec*/
    v23 = sub_3684BC(v35[2]); /*0x2449f4*/
  v24 = netht_ctx_get_instance_576(v23); /*0x2449fc*/
  LODWORD(v35[0]) = 82; /*0x244a04*/
  v25 = 0; /*0x244a14*/
  strcpy((char *)v35 + 4, ";==!"); /*0x244a1c*/
  do /*0x244a7c*/
  {
    *((_BYTE *)v35 + v25 + 4) ^= (_BYTE)v25 + LOBYTE(v35[0]); /*0x244a70*/
    ++v25; /*0x244a74*/
  }
  while ( v25 != 4 ); /*0x244a7c*/
  LOBYTE(v35[1]) = 0; /*0x244a84*/
  v33 = 0; /*0x244a88*/
  v34 = 0; /*0x244a88*/
  v32 = 0; /*0x244a8c*/
  v26 = strlen((const char *)v35 + 4); /*0x244a90*/
  v27 = v26; /*0x244a94*/
  if ( v26 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x244a9c*/
    sub_1EA30(&v32); /*0x244b84*/
  if ( v26 >= 0x17 ) /*0x244aa4*/
  {
    v29 = (v26 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x244ac4*/
    v28 = (char *)sub_368454(v29); /*0x244ad0*/
    v33 = v27; /*0x244ad8*/
    v34 = v28; /*0x244ad8*/
    v32 = v29 | 1; /*0x244adc*/
  }
  else
  {
    v28 = (char *)&v32 + 1; /*0x244ab0*/
    LOBYTE(v32) = 2 * v26; /*0x244ab4*/
    if ( !v26 ) /*0x244ab8*/
      goto LABEL_31; /*0x244ab8*/
  }
  memcpy(v28, (char *)v35 + 4, v27); /*0x244aec*/
LABEL_31:
  v28[v27] = 0; /*0x244af0*/
  v30 = netht_named_key_record(v24, &v32); /*0x244afc*/
  if ( (v32 & 1) != 0 ) /*0x244b04*/
    v30 = sub_3684BC(v34); /*0x244b0c*/
  if ( a5 ) /*0x244b10*/
    *(_QWORD *)(netht_ctx_get_instance_576(v30) + 352) = a5; /*0x244b18*/
  netht_register_probe_scheduler_once(a3, a4); /*0x244b24*/
  byte_4C26C8 = 1; /*0x244b2c*/
  return pthread_rwlock_unlock(&stru_4C2690); /*0x244b68*/
}