_QWORD *sub_24188()
{
  __int64 v0; // x0
  _QWORD *result; // x0
  unsigned __int64 v2; // x8
  __int64 i; // x9
  __int64 j; // x8
  __int64 k; // x8
  __int64 v6; // x0
  __int64 m; // x8
  __int64 v8; // x0
  __int64 nn; // x8
  __int64 v10; // x0
  __int64 i1; // x8
  __int64 i2; // x9
  __int64 v13; // x0
  __int64 v14; // x0
  int v15; // w19
  __int64 n; // x8
  __int64 v17; // x0
  __int64 ii; // x8
  __int64 jj; // x8
  __int64 v20; // x19
  __int64 kk; // x8
  __int64 mm; // x8
  __int64 v23; // x8
  __int64 v24; // x0
  __int64 v25; // x0
  __int64 i5; // x8
  __int64 i6; // x8
  __int64 v28; // x0
  __int64 v29; // x0
  __int64 i3; // x9
  __int64 i4; // x8
  __int64 v32; // x0
  __int64 v33; // x8
  __int64 v34; // x9
  __int64 v35; // x0
  __int64 v36; // x0
  __int64 v37; // x19
  __int64 i7; // x9
  size_t v39; // x0
  size_t v40; // x21
  char *v41; // x22
  unsigned __int64 v42; // x23
  char v43; // w19
  int v44; // w20
  __int64 v45; // x8
  __int64 v46; // x8
  __int64 v47; // x8
  int v48; // w20
  __int64 v49; // x8
  char *v50; // x1
  size_t v51; // x2
  char *v52; // x1
  size_t v53; // x2
  int v54; // w21
  int v55; // w19
  __int64 instance_576; // x0
  __int64 v57; // x0
  __int64 (__fastcall *v58)(_QWORD); // x19
  __int64 v59; // x0
  __int64 v60; // x0
  __int64 v61; // x0
  __int64 v62; // x0
  __int64 v63; // x0
  __int64 i8; // x8
  size_t v65; // x0
  size_t v66; // x19
  char *v67; // x20
  unsigned __int64 v68; // x21
  unsigned __int64 v69; // x9
  __int64 v70; // x0
  __int64 v71; // x19
  char *v72; // x1
  unsigned __int64 v73; // x2
  char *v74; // x0
  unsigned __int64 v75; // x2
  __int64 v76; // x19
  __int64 v77; // x0
  _BYTE v78[24]; // [xsp+8h] [xbp-2D8h] BYREF
  _BYTE v79[24]; // [xsp+20h] [xbp-2C0h] BYREF
  _BYTE v80[24]; // [xsp+38h] [xbp-2A8h] BYREF
  __int128 v81; // [xsp+50h] [xbp-290h] BYREF
  _BYTE *v82; // [xsp+60h] [xbp-280h]
  _QWORD v83[3]; // [xsp+68h] [xbp-278h] BYREF
  int v84; // [xsp+80h] [xbp-260h] BYREF
  _BYTE v85[24]; // [xsp+88h] [xbp-258h] BYREF
  _BYTE v86[24]; // [xsp+A0h] [xbp-240h] BYREF
  _BYTE v87[24]; // [xsp+B8h] [xbp-228h] BYREF
  _BYTE v88[24]; // [xsp+D0h] [xbp-210h] BYREF
  _BYTE v89[28]; // [xsp+E8h] [xbp-1F8h] BYREF
  unsigned __int8 v90; // [xsp+104h] [xbp-1DCh]
  int v91; // [xsp+120h] [xbp-1C0h] BYREF
  _BYTE v92[2]; // [xsp+124h] [xbp-1BCh] BYREF
  char v93[2]; // [xsp+126h] [xbp-1BAh] BYREF
  _QWORD v94[2]; // [xsp+128h] [xbp-1B8h] BYREF
  __int64 v95; // [xsp+138h] [xbp-1A8h]
  __int64 v96; // [xsp+148h] [xbp-198h] BYREF
  size_t v97; // [xsp+150h] [xbp-190h]
  __int64 v98; // [xsp+158h] [xbp-188h]
  __int64 v99; // [xsp+168h] [xbp-178h] BYREF
  unsigned __int64 v100; // [xsp+170h] [xbp-170h]
  char *v101; // [xsp+178h] [xbp-168h]
  __int64 v102; // [xsp+188h] [xbp-158h] BYREF
  size_t v103; // [xsp+190h] [xbp-150h]
  char *v104; // [xsp+198h] [xbp-148h]
  int v105; // [xsp+1A8h] [xbp-138h] BYREF
  char v106[28]; // [xsp+1ACh] [xbp-134h] BYREF
  _QWORD v107[4]; // [xsp+1C8h] [xbp-118h] BYREF
  _OWORD v108[7]; // [xsp+1E8h] [xbp-F8h] BYREF
  __int64 v109; // [xsp+258h] [xbp-88h] BYREF
  __int64 v110; // [xsp+260h] [xbp-80h]
  __int64 v111; // [xsp+268h] [xbp-78h]
  _QWORD v112[5]; // [xsp+278h] [xbp-68h] BYREF

  v112[4] = *(_QWORD *)(_ReadStatusReg(TPIDR_EL0) + 40); /*0x241ac*/
  v81 = 0u; /*0x241b0*/
  v82 = 0; /*0x241b4*/
  v0 = sub_26F618(); /*0x241b8*/
  result = (*(_QWORD *(__fastcall **)(_QWORD *__return_ptr))(*(_QWORD *)v0 + 48LL))(v83); /*0x241c8*/
  if ( (v83[0] & 1) != 0 ) /*0x241dc*/
    v2 = v83[1]; /*0x241dc*/
  else
    v2 = (unsigned __int64)LOBYTE(v83[0]) >> 1; /*0x241dc*/
  if ( !v2 ) /*0x241e0*/
  {
    v15 = 22; /*0x248b0*/
    goto LABEL_98; /*0x248b4*/
  }
  *(_QWORD *)&v108[0] = 0x4750474C00000026LL; /*0x241f4*/
  strcpy((char *)v108 + 8, "\tOI\t`OJC"); /*0x2421c*/
  for ( i = 4; i != 16; ++i ) /*0x24248*/
    *((_BYTE *)v108 + i) ^= LOBYTE(v108[0]); /*0x2425c*/
  LOBYTE(v108[1]) = 0; /*0x2426c*/
  sub_1AF668(&v102, (char *)v108 + 4); /*0x24274*/
  strcpy((char *)v108, "neze3perk3Wxvmrk"); /*0x24284*/
  for ( j = 0; j != 16; ++j ) /*0x242d8*/
    *((_BYTE *)v108 + j) -= 4; /*0x242f8*/
  sub_1AF668(v112, v108); /*0x24314*/
  *(_QWORD *)&v108[0] = 0x62694F2B00000003LL; /*0x24324*/
  strcpy((char *)v108 + 8, "ub,obmd,Pwqjmd8*U"); /*0x24348*/
  for ( k = 4; k != 25; ++k ) /*0x243b0*/
    *((_BYTE *)v108 + k) ^= LOBYTE(v108[0]); /*0x243cc*/
  BYTE9(v108[1]) = 0; /*0x243dc*/
  v6 = sub_1AF33C(&v102, "<init>", (char *)v108 + 4); /*0x243ec*/
  sub_279F0(&v99, &v102, v6, v83); /*0x24400*/
  *(_QWORD *)&v108[0] = 0x6B7D777800000019LL; /*0x24420*/
  strcpy((char *)v108 + 8, "vp}6zvwm|wm6it6Ixzrx~|Ixkj|k"); /*0x24438*/
  for ( m = 0; m != 32; ++m ) /*0x24494*/
    *((_BYTE *)v108 + m + 4) ^= LOBYTE(v108[0]); /*0x244e8*/
  BYTE4(v108[2]) = 0; /*0x244f8*/
  sub_1AF668(&v96, (char *)v108 + 4); /*0x24500*/
  sub_1AE698(&v109); /*0x24508*/
  v8 = sub_1AE698(v94); /*0x24510*/
  if ( (int)sub_1D5E60(v8) < 21 ) /*0x2451c*/
  {
    strcpy((char *)v108, "0Tri~i7tivo7[|zqvoC1^"); /*0x248c4*/
    for ( n = 0; n != 21; ++n ) /*0x2493c*/
      *((_BYTE *)v108 + n) -= 8; /*0x24960*/
    v17 = sub_1AF33C(&v96, "<init>", v108); /*0x24980*/
    sub_279F0(v108, &v96, v17, v83); /*0x24994*/
    sub_1AE94C(&v109, v108); /*0x249a0*/
    sub_1B0760(v108); /*0x249a8*/
    strcpy((char *)v107, "ufwxjUfhpflj"); /*0x249b8*/
    for ( ii = 0; ii != 12; ++ii ) /*0x249f4*/
      *((_BYTE *)v107 + ii) -= 5; /*0x24a14*/
    strcpy( /*0x24a64*/
      (char *)v108,
      "*Nlcxc1kq1Hkng=Nlcxc1ncpi1Uvtkpi=Ncpftqkf1wvkn1Fkurnc{Ogvtkeu=K+Ncpftqkf1eqpvgpv1ro1RcemcigRctugt&Rcemcig=");
    for ( jj = 0; jj != 106; ++jj ) /*0x24bcc*/
      *((_BYTE *)v108 + jj) -= 2; /*0x24c5c*/
    v20 = sub_1AF33C(&v96, v107, v108); /*0x24c80*/
    strcpy((char *)v107, "fsiwtni4zynq4Inxuqf~Rjywnhx"); /*0x24ca0*/
    for ( kk = 0; kk != 27; ++kk ) /*0x24d0c*/
      *((_BYTE *)v107 + kk) -= 5; /*0x24d44*/
    sub_1AF668(v108, v107); /*0x24d5c*/
    sub_1AF73C(v107, v108); /*0x24d68*/
    v105 = 56; /*0x24d70*/
    strcpy(v106, "K\\NoSy[Y!4.77"); /*0x24d88*/
    for ( mm = 0; mm != 13; ++mm ) /*0x24d94*/
      v106[mm] ^= (_BYTE)mm + (_BYTE)v105; /*0x24e80*/
    v106[13] = 0; /*0x24e94*/
    v91 = 57; /*0x24e98*/
    v23 = 0; /*0x24ea4*/
    v92[0] = 17; /*0x24eac*/
    v92[1] = 19; /*0x24ec0*/
    strcpy(v93, "m"); /*0x24edc*/
    do /*0x24f00*/
    {
      v92[v23] ^= (_BYTE)v23 + (_BYTE)v91; /*0x24ef4*/
      ++v23; /*0x24ef8*/
    }
    while ( v23 != 3 ); /*0x24f00*/
    v93[1] = 0; /*0x24f04*/
    v24 = sub_1AF33C(v108, v106, v92); /*0x24f0c*/
    sub_1AE9E0(v107, v24, 0); /*0x24f1c*/
    v91 = 64; /*0x24f24*/
    sub_27B28(&v105, &v109, v20, &v99, v83, v107, &v91); /*0x24f44*/
    sub_1AE94C(v94, &v105); /*0x24f50*/
    sub_1B0760(&v105); /*0x24f58*/
    sub_1B0760(v107); /*0x24f60*/
    v14 = sub_1B0760(v108); /*0x24f68*/
  }
  else
  {
    *(_QWORD *)&v108[0] = 0x7709370000001FLL; /*0x24524*/
    for ( nn = 0; nn != 3; ++nn ) /*0x24530*/
      *((_BYTE *)v108 + nn + 4) ^= (_BYTE)nn + LOBYTE(v108[0]); /*0x24580*/
    BYTE7(v108[0]) = 0; /*0x24590*/
    v10 = sub_1AF33C(&v96, "<init>", (char *)v108 + 4); /*0x245a0*/
    sub_1AFF58(v108, &v96, v10, v107); /*0x245b4*/
    sub_1AE94C(&v109, v108); /*0x245c0*/
    sub_1B0760(v108); /*0x245c8*/
    LODWORD(v107[0]) = 36; /*0x245d0*/
    strcpy((char *)v107 + 4, "TDTTMyKHGLIJ"); /*0x245e8*/
    for ( i1 = 0; i1 != 12; ++i1 ) /*0x245f0*/
      *((_BYTE *)v107 + i1 + 4) ^= (_BYTE)i1 + LOBYTE(v107[0]); /*0x246d0*/
    LOBYTE(v107[2]) = 0; /*0x24708*/
    *(_QWORD *)&v108[0] = 0x6D6640240000000CLL; /*0x2470c*/
    qmemcpy((char *)v108 + 8, "zm#ec#Je`i7E%@mbh~ceh#ocbxibx#|a#\\mogmki\\m~", 43); /*0x2472c*/
    BYTE3(v108[3]) = 127; /*0x24814*/
    strcpy((char *)&v108[3] + 4, "i~(\\mogmki7"); /*0x2481c*/
    for ( i2 = 4; i2 != 63; ++i2 ) /*0x2483c*/
      *((_BYTE *)v108 + i2) ^= LOBYTE(v108[0]); /*0x2485c*/
    HIBYTE(v108[3]) = 0; /*0x2486c*/
    v13 = sub_1AF33C(&v96, (char *)v107 + 4, (char *)v108 + 4); /*0x24874*/
    LODWORD(v107[0]) = 64; /*0x24880*/
    sub_27A8C(v108, &v109, v13, &v99, v107); /*0x24894*/
    sub_1AE94C(v94, v108); /*0x248a0*/
    v14 = sub_1B0760(v108); /*0x248a8*/
  }
  v25 = sub_1D5E60(v14); /*0x24f6c*/
  if ( (int)v25 < 28 ) /*0x24f74*/
  {
    v29 = sub_1D5E60(v25); /*0x25268*/
    if ( (int)v29 < 24 ) /*0x25270*/
    {
      if ( (int)sub_1D5E60(v29) >= 21 ) /*0x2548c*/
      {
        v107[0] = 0x323133380000005BLL; /*0x25494*/
        v33 = 0; /*0x254b4*/
        v107[1] = 0xF1116062115033ALL; /*0x254f8*/
        v107[2] = 0x1E091F0B0A0101LL; /*0x25584*/
        do /*0x25618*/
        {
          *((_BYTE *)v107 + v33 + 4) ^= (_BYTE)v33 + LOBYTE(v107[0]); /*0x2560c*/
          ++v33; /*0x25610*/
        }
        while ( v33 != 19 ); /*0x25618*/
        HIBYTE(v107[2]) = 0; /*0x25624*/
        *(_QWORD *)&v108[0] = 0x202F02660000004ELL; /*0x25654*/
        *((_QWORD *)&v108[0] + 1) = 0x212D612A27213C2ALL; /*0x25670*/
        *(_QWORD *)&v108[1] = 0x233E613A202B3A20LL; /*0x25694*/
        *((_QWORD *)&v108[1] + 1) = 0x2B292F252D2F1E61LL; /*0x256c8*/
        *(_QWORD *)&v108[2] = 0x1E6A3C2B3D3C2F1ELL; /*0x256e8*/
        *((_QWORD *)&v108[2] + 1) = 0x2752B292F252D2FLL; /*0x25718*/
        *(_QWORD *)&v108[3] = 0x612127612F382F24LL; /*0x25748*/
        v34 = 4; /*0x2575c*/
        *((_QWORD *)&v108[3] + 1) = 0x186707752B222708LL; /*0x25770*/
        LOBYTE(v108[4]) = 0; /*0x25790*/
        do /*0x257ac*/
          *((_BYTE *)v108 + v34++) ^= LOBYTE(v108[0]); /*0x257a0*/
        while ( v34 != 64 ); /*0x257ac*/
        LOBYTE(v108[4]) = 0; /*0x257b0*/
        v35 = sub_1AF8D4(&v96, (char *)v107 + 4, (char *)v108 + 4); /*0x257b8*/
        LODWORD(v108[0]) = 0; /*0x257c0*/
        sub_27D2C(&v96, v35, v94, &v99, v108); /*0x257d4*/
      }
    }
    else
    {
      LODWORD(v107[0]) = 56; /*0x2528c*/
      strcpy((char *)v107 + 4, "[WTT][L{]JLQ^Q[YL]K"); /*0x25294*/
      for ( i3 = 4; i3 != 23; ++i3 ) /*0x252fc*/
        *((_BYTE *)v107 + i3) ^= LOBYTE(v107[0]); /*0x25310*/
      HIBYTE(v107[2]) = 0; /*0x25340*/
      strcpy((char *)v108, "*Ncpftqkf1eqpvgpv1ro1RcemcigRctugt&Rcemcig=K+X"); /*0x25344*/
      for ( i4 = 0; i4 != 46; ++i4 ) /*0x25400*/
        *((_BYTE *)v108 + i4) -= 2; /*0x2544c*/
      v32 = sub_1AF8D4(&v96, (char *)v107 + 4, v108); /*0x25464*/
      LODWORD(v108[0]) = 0; /*0x2546c*/
      sub_27C98(&v96, v32, v94, v108); /*0x2547c*/
    }
  }
  else
  {
    LODWORD(v107[0]) = 42; /*0x24f7c*/
    strcpy((char *)v107 + 4, "ID@AKLDrWA@\\P^[XN^O"); /*0x24f94*/
    for ( i5 = 0; i5 != 19; ++i5 ) /*0x24f9c*/
      *((_BYTE *)v107 + i5 + 4) ^= (_BYTE)i5 + LOBYTE(v107[0]); /*0x250f4*/
    HIBYTE(v107[2]) = 0; /*0x25124*/
    strcpy((char *)v108, "0Tivlzwql7kwv|mv|7xu7XiksiomXiz{mz,XiksiomCb1^"); /*0x25128*/
    for ( i6 = 0; i6 != 46; ++i6 ) /*0x251e4*/
      *((_BYTE *)v108 + i6) -= 8; /*0x25230*/
    v28 = sub_1AF8D4(&v96, (char *)v107 + 4, v108); /*0x25248*/
    LOBYTE(v108[0]) = 0; /*0x25250*/
    sub_27C04(&v96, v28, v94, v108); /*0x25260*/
  }
  sub_1B0760(v94); /*0x257dc*/
  sub_1B0760(&v109); /*0x257e4*/
  sub_1B0760(&v96); /*0x257ec*/
  sub_1B0760(&v99); /*0x257f4*/
  sub_1B0760(v112); /*0x257fc*/
  sub_1B0760(&v102); /*0x25804*/
  v96 = 0; /*0x25808*/
  v97 = 0; /*0x25808*/
  v98 = 0; /*0x2580c*/
  v109 = 0; /*0x25810*/
  v110 = 0; /*0x25810*/
  v111 = 0; /*0x25814*/
  v94[0] = 0; /*0x25818*/
  v94[1] = 0; /*0x25818*/
  v95 = 0; /*0x2581c*/
  v36 = sub_26F618(); /*0x25820*/
  result = (_QWORD *)(*(__int64 (__fastcall **)(__int64, __int64 *, __int64 *, _QWORD *))(*(_QWORD *)v36 + 160LL))( /*0x25838*/
                       v36,
                       &v96,
                       &v109,
                       v94);
  if ( (int)result > 1 ) /*0x25840*/
  {
    v15 = 0; /*0x25844*/
    goto LABEL_89; /*0x25848*/
  }
  v37 = sub_2A837C(result); /*0x25850*/
  *(_QWORD *)&v108[0] = 0x2665676B00000008LL; /*0x25878*/
  strcpy((char *)v108 + 8, "fm|mi{m&`|xzg|mk|&xgdq&i"); /*0x25894*/
  for ( i7 = 4; i7 != 32; ++i7 ) /*0x25904*/
    *((_BYTE *)v108 + i7) ^= LOBYTE(v108[0]); /*0x25920*/
  LOBYTE(v108[2]) = 0; /*0x25934*/
  memset(v112, 0, 24); /*0x2593c*/
  v39 = strlen((const char *)v108 + 4); /*0x25940*/
  v40 = v39; /*0x25944*/
  if ( v39 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x2594c*/
    sub_1EA30(v112); /*0x263a8*/
  if ( v39 >= 0x17 ) /*0x25954*/
  {
    v42 = (v39 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x25974*/
    v41 = (char *)sub_368454(v42); /*0x25980*/
    v112[1] = v40; /*0x25988*/
    v112[2] = v41; /*0x25988*/
    v112[0] = v42 | 1; /*0x2598c*/
    goto LABEL_62; /*0x2598c*/
  }
  v41 = (char *)v112 + 1; /*0x25960*/
  LOBYTE(v112[0]) = 2 * v39; /*0x25964*/
  if ( v39 ) /*0x25968*/
LABEL_62:
    memcpy(v41, (char *)v108 + 4, v40); /*0x25990*/
  v41[v40] = 0; /*0x259a0*/
  sub_2AC660(&v102, v37, v112); /*0x259b0*/
  if ( (v112[0] & 1) != 0 ) /*0x259b8*/
    sub_3684BC(v112[2]); /*0x259c0*/
  sub_1AE7AC(v108, 0, 0); /*0x259d0*/
  if ( (sub_1AE90C(&v102, v108) & 1) != 0 ) /*0x259e0*/
  {
    sub_1B0760(v108); /*0x259e8*/
LABEL_68:
    v15 = 0; /*0x25a08*/
    v44 = 1; /*0x25a0c*/
    goto LABEL_88; /*0x25a10*/
  }
  v43 = sub_1AE730(&v102); /*0x259f8*/
  sub_1B0760(v108); /*0x25a00*/
  if ( (v43 & 1) != 0 ) /*0x25a04*/
    goto LABEL_68; /*0x25a04*/
  sub_1AF70C(v112, v103, 1); /*0x25a20*/
  if ( (sub_1AE730(v112) & 1) != 0 ) /*0x25a2c*/
  {
    v15 = 0; /*0x25a30*/
    v44 = 1; /*0x25a34*/
  }
  else
  {
    *(_QWORD *)&v108[0] = 0x86C373200000044LL; /*0x25a68*/
    *((_QWORD *)&v108[0] + 1) = 0x2A25286B2532252ELL; /*0x25a84*/
    *(_QWORD *)&v108[1] = 0x232A2D3630176B23LL; /*0x25aac*/
    DWORD2(v108[1]) = 1829571967; /*0x25ad8*/
    BYTE12(v108[1]) = 8; /*0x25af0*/
    qmemcpy((char *)&v108[1] + 13, "'+)k*!0!%7!k,046+0!'0k6!71(0k", 29); /*0x25af8*/
    v45 = 0; /*0x25b3c*/
    WORD5(v108[3]) = 5650; /*0x25b90*/
    qmemcpy((char *)&v108[3] + 12, "!71(0", 5); /*0x25b98*/
    *(_WORD *)((char *)&v108[4] + 1) = 127; /*0x25bac*/
    do /*0x25bcc*/
      *((_BYTE *)v108 + v45++ + 4) ^= LOBYTE(v108[0]); /*0x25bc0*/
    while ( v45 != 62 ); /*0x25bcc*/
    BYTE2(v108[4]) = 0; /*0x25bd8*/
    LODWORD(v107[0]) = 19; /*0x25bdc*/
    v105 = 34; /*0x25be0*/
    sub_27DAC(&v99, (int)v112, (char *)v108 + 4); /*0x25bf8*/
    if ( (sub_1AE730(&v99) & 1) != 0 ) /*0x25c04*/
    {
      v15 = 0; /*0x25c08*/
      v44 = 1; /*0x25c0c*/
    }
    else
    {
      v46 = 0; /*0x25c18*/
      strcpy((char *)v108, "yl{"); /*0x25c24*/
      do /*0x25c4c*/
        *((_BYTE *)v108 + v46++) -= 7; /*0x25c40*/
      while ( v46 != 3 ); /*0x25c4c*/
      v15 = sub_2801C(&v99, v108); /*0x25c5c*/
      if ( v15 ) /*0x25c60*/
      {
        v47 = 0; /*0x25c68*/
        LODWORD(v107[0]) = 90; /*0x25c80*/
        strcpy((char *)v107 + 4, "(?)"); /*0x25c84*/
        do /*0x25cac*/
          *((_BYTE *)v107 + v47++ + 4) ^= LOBYTE(v107[0]); /*0x25ca0*/
        while ( v47 != 3 ); /*0x25cac*/
        HIBYTE(v107[0]) = 0; /*0x25cb0*/
        sub_280C4(v108, &v99); /*0x25cbc*/
        if ( (v81 & 1) != 0 ) /*0x25cc4*/
        {
          *v82 = 0; /*0x25cdc*/
          *((_QWORD *)&v81 + 1) = 0; /*0x25ce4*/
          if ( (v81 & 1) != 0 ) /*0x25ce8*/
          {
            sub_3684BC(v82); /*0x25cf0*/
            *(_QWORD *)&v81 = 0; /*0x25cf4*/
          }
        }
        else
        {
          LOWORD(v81) = 0; /*0x25cc8*/
        }
        v44 = 0; /*0x25d00*/
        v82 = *(_BYTE **)&v108[1]; /*0x25d04*/
        v81 = v108[0]; /*0x25d08*/
      }
      else
      {
        v44 = 0; /*0x25cd0*/
      }
    }
    sub_1B0760(&v99); /*0x25d10*/
  }
  sub_1B0760(v112); /*0x25d18*/
LABEL_88:
  result = (_QWORD *)sub_1B0760(&v102); /*0x25d1c*/
  if ( !v44 ) /*0x25d24*/
  {
    v48 = 0; /*0x264a8*/
    goto LABEL_90; /*0x264ac*/
  }
LABEL_89:
  v48 = 1; /*0x25d28*/
LABEL_90:
  if ( (v94[0] & 1) != 0 ) /*0x25d30*/
    result = (_QWORD *)sub_3684BC(v95); /*0x25d38*/
  if ( (v109 & 1) != 0 ) /*0x25d40*/
    result = (_QWORD *)sub_3684BC(v111); /*0x25d48*/
  if ( (v96 & 1) != 0 ) /*0x25d50*/
    result = (_QWORD *)sub_3684BC(v98); /*0x25d58*/
  if ( v48 ) /*0x25d5c*/
    v15 = 0; /*0x25d60*/
LABEL_98:
  if ( (v83[0] & 1) != 0 ) /*0x25d68*/
    result = (_QWORD *)sub_3684BC(v83[2]); /*0x25d70*/
  if ( v15 >= 1 ) /*0x25d78*/
  {
    v109 = 0x343D3C340000005CLL; /*0x25d80*/
    v49 = 0; /*0x25da4*/
    v110 = 0xF163B101B123FLL; /*0x25dec*/
    LOWORD(v111) = 6; /*0x25e80*/
    do /*0x25ea4*/
    {
      *((_BYTE *)&v109 + v49 + 4) ^= (_BYTE)v49 + (_BYTE)v109; /*0x25e98*/
      ++v49; /*0x25e9c*/
    }
    while ( v49 != 13 ); /*0x25ea4*/
    BYTE1(v111) = 0; /*0x25ea8*/
    sub_1E300(v80, (char *)&v109 + 4, v94); /*0x25eb4*/
    sub_364088(&v96, (unsigned int)v15); /*0x25ec4*/
    if ( (v96 & 1) != 0 ) /*0x25edc*/
      LODWORD(v50) = v98; /*0x25edc*/
    else
      v50 = (char *)&v96 + 1; /*0x25edc*/
    if ( (v96 & 1) != 0 ) /*0x25ee0*/
      v51 = v97; /*0x25ee0*/
    else
      v51 = (unsigned __int64)(unsigned __int8)v96 >> 1; /*0x25ee0*/
    sub_1F560(v79, (int)v50, v51); /*0x25eec*/
    if ( (v81 & 1) != 0 ) /*0x25f08*/
      LODWORD(v52) = (_DWORD)v82; /*0x25f08*/
    else
      v52 = (char *)&v81 + 1; /*0x25f08*/
    if ( (v81 & 1) != 0 ) /*0x25f0c*/
      v53 = *((_QWORD *)&v81 + 1); /*0x25f0c*/
    else
      v53 = (unsigned __int64)(unsigned __int8)v81 >> 1; /*0x25f0c*/
    sub_1F560(v78, (int)v52, v53); /*0x25f18*/
    sub_2780C(&v84, 30, v80, v79, v78, 0, 1); /*0x25f38*/
    v54 = *(unsigned __int8 *)(sub_26173C() + 167); /*0x25f40*/
    v55 = v90; /*0x25f44*/
    v90 &= v54; /*0x25f4c*/
    result = (_QWORD *)sub_2F3308((__int64)&v84); /*0x25f54*/
    v90 = v55; /*0x25f58*/
    if ( v55 ) /*0x25f5c*/
    {
      HIBYTE(v108[1]) = 23; /*0x25f78*/
      BYTE8(v108[0]) = 0; /*0x25f7c*/
      LODWORD(v108[0]) = v84; /*0x25f80*/
      sub_1EB70((char *)v108 + 8, v85); /*0x25f88*/
      DWORD1(v108[0]) = (*(__int64 (__fastcall **)(_QWORD))(qword_4C2298 + 400))(0); /*0x25fa4*/
      netht_ctx_get_instance_576(); /*0x25fa8*/
      result = (_QWORD *)sub_266F14(v108); /*0x25fb0*/
      LOBYTE(v55) = (_BYTE)result; /*0x25fb4*/
      if ( HIBYTE(v108[1]) >= 0x40u ) /*0x25fc0*/
        result = (_QWORD *)sub_1F748((char *)v108 + 8); /*0x25fc8*/
    }
    if ( !v54 ) /*0x25fcc*/
    {
      if ( (v55 & 1) != 0 ) /*0x26138*/
        result = (_QWORD *)sub_230B68(7); /*0x26140*/
      goto LABEL_157; /*0x26144*/
    }
    if ( !v90 ) /*0x25fd4*/
    {
LABEL_157:
      if ( v89[23] >= 0x40u ) /*0x262b4*/
        result = (_QWORD *)sub_1F748(v89); /*0x262bc*/
      if ( v88[23] >= 0x40u ) /*0x262c8*/
        result = (_QWORD *)sub_1F748(v88); /*0x262d0*/
      if ( v87[23] >= 0x40u ) /*0x262e0*/
        result = (_QWORD *)sub_1F748(v87); /*0x262e8*/
      if ( v86[23] >= 0x40u ) /*0x262f4*/
        result = (_QWORD *)sub_1F748(v86); /*0x262fc*/
      if ( v85[23] >= 0x40u ) /*0x26308*/
        result = (_QWORD *)sub_1F748(v85); /*0x26314*/
      if ( v78[23] >= 0x40u ) /*0x26320*/
        result = (_QWORD *)sub_1F748(v78); /*0x26328*/
      if ( v79[23] >= 0x40u ) /*0x26334*/
        result = (_QWORD *)sub_1F748(v79); /*0x2633c*/
      if ( (v96 & 1) != 0 ) /*0x26344*/
        result = (_QWORD *)sub_3684BC(v98); /*0x2634c*/
      if ( v80[23] >= 0x40u ) /*0x26358*/
        result = (_QWORD *)sub_1F748(v80); /*0x26360*/
      goto LABEL_175; /*0x26360*/
    }
    sub_230B68(1); /*0x25fdc*/
    instance_576 = netht_ctx_get_instance_576(); /*0x25fe0*/
    v57 = sub_2653A8(instance_576, 1); /*0x25fe8*/
    if ( v84 != 25 ) /*0x25ff4*/
    {
      v58 = *(__int64 (__fastcall **)(_QWORD))(qword_4C2298 + 8); /*0x26004*/
      v59 = sub_26173C(); /*0x26008*/
      v57 = v58(*(unsigned int *)(v59 + 108)); /*0x26010*/
    }
    v60 = sub_91EE0(v57); /*0x26014*/
    v61 = sub_9239C(v60, 307062571); /*0x26020*/
    v62 = sub_12E744(v61); /*0x26024*/
    sub_364798(&v99, v62); /*0x2602c*/
    v63 = sub_26F618(); /*0x26030*/
    (*(void (__fastcall **)(_OWORD *__return_ptr))(*(_QWORD *)v63 + 8LL))(v108); /*0x26040*/
    strcpy((char *)v112, "10jtgeqtf;78f5ygjh3ih"); /*0x26054*/
    for ( i8 = 0; i8 != 21; ++i8 ) /*0x260c0*/
      *((_BYTE *)v112 + i8) -= 2; /*0x260ec*/
    v102 = 0; /*0x26100*/
    v103 = 0; /*0x26100*/
    v104 = 0; /*0x26104*/
    v65 = strlen((const char *)v112); /*0x26108*/
    v66 = v65; /*0x2610c*/
    if ( v65 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x26114*/
      sub_1EA30(&v102); /*0x263b0*/
    if ( v65 >= 0x17 ) /*0x2611c*/
    {
      v68 = (v65 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x2614c*/
      v67 = (char *)sub_368454(v68); /*0x26158*/
      v103 = v66; /*0x26160*/
      v104 = v67; /*0x26160*/
      v102 = v68 | 1; /*0x26164*/
    }
    else
    {
      v67 = (char *)&v102 + 1; /*0x26128*/
      LOBYTE(v102) = 2 * v65; /*0x2612c*/
      if ( !v65 ) /*0x26130*/
        goto LABEL_132; /*0x26130*/
    }
    memcpy(v67, v112, v66); /*0x26174*/
LABEL_132:
    v67[v66] = 0; /*0x26178*/
    sub_23640(v112, v108, &v102); /*0x26188*/
    if ( (v99 & 1) != 0 ) /*0x261a8*/
      v69 = v100; /*0x261a8*/
    else
      v69 = (unsigned __int64)(unsigned __int8)v99 >> 1; /*0x261a8*/
    v70 = (*(__int64 (__fastcall **)(unsigned __int64))(qword_4C2298 + 352))(v69 + 1); /*0x261b4*/
    v71 = v70; /*0x261b8*/
    if ( v70 ) /*0x261bc*/
    {
      if ( (v99 & 1) != 0 ) /*0x261e0*/
        v72 = v101; /*0x261e0*/
      else
        v72 = (char *)&v99 + 1; /*0x261e0*/
      if ( (v99 & 1) != 0 ) /*0x261e4*/
        v73 = v100; /*0x261e4*/
      else
        v73 = (unsigned __int64)(unsigned __int8)v99 >> 1; /*0x261e4*/
      (*(void (__fastcall **)(__int64, char *, unsigned __int64))(qword_4C2298 + 392))(v70, v72, v73); /*0x261ec*/
      if ( (v112[0] & 1) != 0 ) /*0x26210*/
        v74 = (char *)v112[2]; /*0x26210*/
      else
        v74 = (char *)v112 + 1; /*0x26210*/
      if ( (v99 & 1) != 0 ) /*0x26218*/
        v75 = v100; /*0x26218*/
      else
        v75 = (unsigned __int64)(unsigned __int8)v99 >> 1; /*0x26218*/
      sub_2E676C(v74, v71, v75); /*0x26220*/
      v70 = (*(__int64 (__fastcall **)(__int64))(qword_4C2298 + 360))(v71); /*0x26230*/
    }
    if ( (v112[0] & 1) != 0 ) /*0x26238*/
      v70 = sub_3684BC(v112[2]); /*0x26240*/
    if ( (v102 & 1) != 0 ) /*0x26248*/
      v70 = sub_3684BC(v104); /*0x26250*/
    if ( (v108[0] & 1) != 0 ) /*0x26258*/
      v70 = sub_3684BC(*(_QWORD *)&v108[1]); /*0x26260*/
    v76 = qword_4C26F8; /*0x2626c*/
    v77 = sub_343F98(v70); /*0x26270*/
    sub_23A04(v76, v77, &loc_23748); /*0x26284*/
    result = (_QWORD *)(*(__int64 (__fastcall **)(_QWORD))(qword_4C2298 + 600))(0); /*0x26294*/
    if ( (v99 & 1) != 0 ) /*0x2629c*/
      result = (_QWORD *)sub_3684BC(v101); /*0x262a4*/
    goto LABEL_157; /*0x262a4*/
  }
LABEL_175:
  if ( (v81 & 1) != 0 ) /*0x26368*/
    return (_QWORD *)sub_3684BC(v82); /*0x26370*/
  return result; /*0x2639c*/
}