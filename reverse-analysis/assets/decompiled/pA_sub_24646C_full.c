__int64 *__usercall sub_24646C@<X0>(char *a1@<X1>, __int64 *result@<X0>, _QWORD *a3@<X8>)
{
  int v4; // w21
  size_t v6; // x0
  size_t v7; // x21
  char *v8; // x22
  __int64 v9; // x8
  size_t v10; // x0
  __int64 v11; // x0
  __int64 v12; // x0
  unsigned __int64 v13; // x23
  __int64 ii; // x8
  _QWORD *v15; // x0
  __int64 v16; // x0
  __int128 v17; // q0
  char v18; // w9
  __int64 n; // x9
  size_t v20; // x0
  __int64 v21; // x0
  _QWORD *v22; // x0
  __int64 v23; // x0
  __int64 v24; // x0
  __int64 v25; // x0
  __int64 v26; // x0
  __int64 v27; // x0
  _QWORD *v28; // x0
  __int64 v29; // x0
  __int64 v30; // x0
  _QWORD *v31; // x0
  __int64 v32; // x20
  size_t v33; // x0
  size_t v34; // x21
  char *v35; // x22
  const char *v36; // x21
  __int64 v37; // x20
  size_t v38; // x0
  size_t v39; // x22
  char *v40; // x23
  __int64 v41; // x0
  __int64 i; // x8
  __int64 v43; // x8
  size_t v44; // x0
  size_t v45; // x0
  size_t v46; // x21
  char *v47; // x22
  size_t v48; // x0
  size_t v49; // x21
  char *v50; // x22
  size_t v51; // x0
  size_t v52; // x21
  char *v53; // x22
  __int64 v54; // x0
  __int64 v55; // x0
  size_t v56; // x0
  size_t v57; // x21
  char *v58; // x22
  __int64 v59; // x0
  __int64 v60; // x0
  unsigned __int64 v61; // x24
  __int64 v62; // x0
  size_t v63; // x0
  __int64 j; // x8
  size_t v65; // x0
  __int64 v66; // x0
  unsigned __int64 v67; // x23
  __int64 v68; // x0
  unsigned __int64 v69; // x23
  __int64 v70; // x8
  char *v71; // x1
  size_t v72; // x2
  _BYTE *v73; // x0
  unsigned __int64 v74; // x23
  __int64 m; // x8
  char *v76; // x1
  size_t v77; // x2
  unsigned __int64 v78; // x23
  __int64 k; // x8
  char *v80; // x1
  size_t v81; // x2
  unsigned __int64 v82; // x23
  __int64 v83; // x0
  int v84; // w20
  __int64 v85; // x0
  __int64 v86; // x0
  int v87; // w8
  __int64 v88; // x0
  _BYTE *v89; // x8
  __int128 v90; // q0
  _BYTE v91[24]; // [xsp+1B0h] [xbp-1E8h] BYREF
  _BYTE v92[24]; // [xsp+1C8h] [xbp-1D0h] BYREF
  _BYTE v93[24]; // [xsp+1E0h] [xbp-1B8h] BYREF
  _BYTE v94[24]; // [xsp+1F8h] [xbp-1A0h] BYREF
  _BYTE v95[24]; // [xsp+210h] [xbp-188h] BYREF
  char v96[8]; // [xsp+228h] [xbp-170h] BYREF
  _BYTE v97[24]; // [xsp+230h] [xbp-168h] BYREF
  __int128 v98; // [xsp+248h] [xbp-150h] BYREF
  _BYTE *v99; // [xsp+258h] [xbp-140h]
  char src[16]; // [xsp+268h] [xbp-130h] BYREF
  char *v101; // [xsp+278h] [xbp-120h]
  _DWORD v102[4]; // [xsp+280h] [xbp-118h] BYREF
  char v103; // [xsp+290h] [xbp-108h]
  char v104; // [xsp+291h] [xbp-107h]
  char v105; // [xsp+292h] [xbp-106h]
  char v106; // [xsp+293h] [xbp-105h]
  char v107[16]; // [xsp+298h] [xbp-100h] BYREF
  __int64 v108; // [xsp+2A8h] [xbp-F0h]
  __int64 v109; // [xsp+2B0h] [xbp-E8h]
  int v110; // [xsp+2B8h] [xbp-E0h] BYREF
  unsigned __int8 v111; // [xsp+2CFh] [xbp-C9h]
  __int64 v112; // [xsp+2D0h] [xbp-C8h] BYREF
  unsigned __int8 v113; // [xsp+2E7h] [xbp-B1h]
  _QWORD v114[2]; // [xsp+2E8h] [xbp-B0h] BYREF
  unsigned __int8 v115; // [xsp+2FFh] [xbp-99h]
  __int64 v116; // [xsp+300h] [xbp-98h] BYREF
  unsigned __int8 v117; // [xsp+317h] [xbp-81h]

  v4 = (int)result; /*0x246498*/
  _ReadStatusReg(TPIDR_EL0); /*0x24648c*/
  if ( (byte_4C26C8 & 1) == 0 ) /*0x2464ac*/
  {
    *a3 = 0; /*0x246504*/
    a3[1] = 0; /*0x246504*/
    a3[2] = 0; /*0x246508*/
    return result; /*0x24650c*/
  }
  v98 = 0u; /*0x2464b8*/
  v99 = 0; /*0x2464bc*/
  if ( (_DWORD)result != 100 ) /*0x2464c0*/
  {
    v9 = 0; /*0x246514*/
    strcpy((char *)v102, "nthd"); /*0x246524*/
    do /*0x246550*/
      *((_BYTE *)v102 + v9++) -= 5; /*0x246544*/
    while ( v9 != 4 ); /*0x246550*/
    sub_364088(src, (unsigned int)result); /*0x24655c*/
    v10 = strlen((const char *)v102); /*0x246564*/
    v11 = sub_2840C((int)src, 0, v102, v10); /*0x246578*/
    v108 = *(_QWORD *)(v11 + 16); /*0x246580*/
    *(_OWORD *)v107 = *(_OWORD *)v11; /*0x246588*/
    *(_QWORD *)(v11 + 8) = 0; /*0x24658c*/
    *(_QWORD *)(v11 + 16) = 0; /*0x24658c*/
    *(_QWORD *)v11 = 0; /*0x246590*/
    if ( (src[0] & 1) != 0 ) /*0x246598*/
      v11 = sub_3684BC(v101); /*0x2465a0*/
    v12 = netht_ctx_get_instance_576(v11); /*0x2465a4*/
    result = (__int64 *)netht_named_key_record(v12, v107); /*0x2465ac*/
    if ( (v107[0] & 1) != 0 ) /*0x2465b4*/
      result = (__int64 *)sub_3684BC(v108); /*0x2465bc*/
    switch ( v4 ) /*0x2465dc*/
    {
      case 1: /*0x2465dc*/
        v41 = sub_26EBB8(result); /*0x24695c*/
        (*(void (__fastcall **)(char *__return_ptr))(*(_QWORD *)v41 + 8LL))(src); /*0x24696c*/
        sub_41E28(v107, src); /*0x246978*/
        if ( HIBYTE(v101) >= 0x40u ) /*0x246984*/
          sub_1F748(src); /*0x24698c*/
        strcpy((char *)v102, "nv~uj}x{C"); /*0x246994*/
        for ( i = 0; i != 9; ++i ) /*0x2469d4*/
          *((_BYTE *)v102 + i) -= 9; /*0x2469ec*/
        result = sub_5956C(src, v102, v107); /*0x246a08*/
        goto LABEL_188; /*0x246a0c*/
      case 2: /*0x2465dc*/
        sub_164950(v107); /*0x246a14*/
        if ( *(int *)v107 < 1 ) /*0x246a20*/
        {
          *(_DWORD *)src = 28; /*0x246d50*/
          strcpy(&src[4], "nssh&,"); /*0x246d58*/
          for ( j = 0; j != 6; ++j ) /*0x246d84*/
            src[j + 4] ^= src[0]; /*0x246d9c*/
          src[10] = 0; /*0x246db0*/
          v65 = strlen(&src[4]); /*0x246db4*/
          result = (__int64 *)sub_1FDD0((int)&v98, &src[4], v65); /*0x246dc4*/
        }
        else
        {
          v43 = 0; /*0x246a28*/
          strcpy(src, "urrw=4"); /*0x246a3c*/
          do /*0x246a70*/
            src[v43++] -= 3; /*0x246a64*/
          while ( v43 != 6 ); /*0x246a70*/
          v44 = strlen(src); /*0x246a78*/
          result = (__int64 *)sub_1FDD0((int)&v98, src, v44); /*0x246a88*/
        }
        if ( (v107[8] & 1) == 0 ) /*0x246dcc*/
          goto LABEL_195; /*0x246dcc*/
        v66 = v109; /*0x246dd0*/
        goto LABEL_194; /*0x246dd4*/
      case 3: /*0x2465dc*/
        v29 = sub_2A2884(result); /*0x246860*/
        result = (*(_QWORD *(__fastcall **)(char *__return_ptr))(*(_QWORD *)v29 + 48LL))(v107); /*0x246870*/
        goto LABEL_81; /*0x246874*/
      case 4: /*0x2465dc*/
      case 5: /*0x2465dc*/
      case 6: /*0x2465dc*/
      case 10: /*0x2465dc*/
      case 11: /*0x2465dc*/
      case 15: /*0x2465dc*/
        result = (__int64 *)sub_1FDD0((int)&v98, &unk_39DD38, 0); /*0x2465f0*/
        goto LABEL_195; /*0x2465f4*/
      case 7: /*0x2465dc*/
        result = sub_264E40(v107, result); /*0x246b68*/
        goto LABEL_81; /*0x246b6c*/
      case 8: /*0x2465dc*/
        if ( a1 ) /*0x24690c*/
          v36 = a1; /*0x24690c*/
        else
          v36 = (const char *)&unk_39DD38; /*0x24690c*/
        LOBYTE(v102[0]) = 0; /*0x246910*/
        *(_QWORD *)src = 0; /*0x246914*/
        v37 = sub_305D9C(result); /*0x24691c*/
        memset(v107, 0, sizeof(v107)); /*0x246924*/
        v108 = 0; /*0x246928*/
        v38 = strlen(v36); /*0x24692c*/
        v39 = v38; /*0x246930*/
        if ( v38 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x246938*/
          sub_1EA30(v107); /*0x247514*/
        if ( v38 >= 0x17 ) /*0x246940*/
        {
          v61 = (v38 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x246c4c*/
          v40 = (char *)sub_368454(v61); /*0x246c58*/
          *(_QWORD *)&v107[8] = v39; /*0x246c60*/
          v108 = (__int64)v40; /*0x246c60*/
          *(_QWORD *)v107 = v61 | 1; /*0x246c64*/
        }
        else
        {
          v40 = &v107[1]; /*0x24694c*/
          v107[0] = 2 * v38; /*0x246950*/
          if ( !v38 ) /*0x246954*/
            goto LABEL_88; /*0x246954*/
        }
        memcpy(v40, v36, v39); /*0x246c74*/
LABEL_88:
        v40[v39] = 0; /*0x246c78*/
        sub_30974C(v37, v107, v102, src); /*0x246c90*/
        if ( (v107[0] & 1) != 0 ) /*0x246c98*/
          sub_3684BC(v108); /*0x246ca0*/
        *(_QWORD *)&v107[8] = 0; /*0x246cb4*/
        v108 = 0; /*0x246cb8*/
        *(_QWORD *)v107 = 0x6E656B6F740ALL; /*0x246cbc*/
        result = (__int64 *)sub_2E92BC(v107, a3); /*0x246cd0*/
        goto LABEL_23; /*0x246cd4*/
      case 9: /*0x2465dc*/
        sub_264E40(v107, result); /*0x246bb4*/
        result = sub_2E8464((__int64 *)src, (int)v107); /*0x246bc0*/
        goto LABEL_188; /*0x246bc4*/
      case 12: /*0x2465dc*/
        v25 = sub_287DA4(result); /*0x246810*/
        result = (*(_QWORD *(__fastcall **)(char *__return_ptr))(*(_QWORD *)v25 + 32LL))(v107); /*0x246820*/
        goto LABEL_81; /*0x246824*/
      case 13: /*0x2465dc*/
        v26 = netht_ctx_get_instance_576(result); /*0x246828*/
        v27 = sub_2653A0(v26); /*0x24682c*/
        v28 = sub_364088(v107, v27); /*0x246834*/
        if ( (v98 & 1) != 0 ) /*0x24683c*/
        {
          *v99 = 0; /*0x246cdc*/
          *((_QWORD *)&v98 + 1) = 0; /*0x246ce4*/
          if ( (v98 & 1) != 0 ) /*0x246ce8*/
          {
            v28 = (_QWORD *)sub_3684BC(v99); /*0x246cf0*/
            *(_QWORD *)&v98 = 0; /*0x246cf4*/
          }
        }
        else
        {
          LOWORD(v98) = 0; /*0x246840*/
        }
        v99 = (_BYTE *)v108; /*0x246d00*/
        v98 = *(_OWORD *)v107; /*0x246d04*/
        v62 = netht_ctx_get_instance_576(v28); /*0x246d08*/
        result = (__int64 *)sub_265398(v62, 0); /*0x246d10*/
        goto LABEL_195; /*0x246d14*/
      case 14: /*0x2465dc*/
        v54 = netht_ctx_get_instance_576(result); /*0x246b50*/
        v55 = sub_2653CC(v54); /*0x246b54*/
        result = sub_364088(v107, v55); /*0x246b5c*/
        goto LABEL_81; /*0x246b60*/
      case 16: /*0x2465dc*/
        if ( !a1 || (result = (__int64 *)(*(__int64 (__fastcall **)(char *))(qword_4C2298 + 152))(a1)) == 0 ) /*0x246894*/
        {
          v60 = netht_ctx_get_instance_576(result); /*0x246bf8*/
          result = sub_265BA0(v107, v60); /*0x246c00*/
LABEL_81:
          if ( (v98 & 1) != 0 ) /*0x246c08*/
          {
            *v99 = 0; /*0x246c18*/
            *((_QWORD *)&v98 + 1) = 0; /*0x246c20*/
            if ( (v98 & 1) != 0 ) /*0x246c24*/
            {
              result = (__int64 *)sub_3684BC(v99); /*0x246c2c*/
              *(_QWORD *)&v98 = 0; /*0x246c30*/
            }
          }
          else
          {
            LOWORD(v98) = 0; /*0x246c0c*/
          }
          v99 = (_BYTE *)v108; /*0x246c3c*/
          v98 = *(_OWORD *)v107; /*0x246c40*/
          goto LABEL_195; /*0x246c44*/
        }
        v30 = (*(__int64 (__fastcall **)(char *))(qword_4C2298 + 152))(a1); /*0x2468a4*/
        v31 = sub_240D28(v107, a1, v30); /*0x2468b4*/
        v32 = sub_30CF8C(v31); /*0x2468bc*/
        LOBYTE(v102[0]) = 0; /*0x2468c4*/
        v101 = 0; /*0x2468c8*/
        memset(src, 0, sizeof(src)); /*0x2468cc*/
        v33 = strlen((const char *)v102); /*0x2468d0*/
        v34 = v33; /*0x2468d4*/
        if ( v33 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x2468dc*/
          sub_1EA30(src); /*0x24753c*/
        if ( v33 >= 0x17 ) /*0x2468e4*/
        {
          v82 = (v33 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x2473d0*/
          v35 = (char *)sub_368454(v82); /*0x2473dc*/
          *(_QWORD *)&src[8] = v34; /*0x2473e4*/
          v101 = v35; /*0x2473e4*/
          *(_QWORD *)src = v82 | 1; /*0x2473e8*/
        }
        else
        {
          v35 = &src[1]; /*0x2468f0*/
          src[0] = 2 * v33; /*0x2468f4*/
          if ( !v33 ) /*0x2468f8*/
            goto LABEL_181; /*0x2468f8*/
        }
        memcpy(v35, v102, v34); /*0x2473f8*/
LABEL_181:
        v35[v34] = 0; /*0x2473fc*/
        v83 = sub_30D808(v32, v107, src); /*0x24740c*/
        v84 = v83; /*0x247410*/
        if ( (src[0] & 1) != 0 ) /*0x247418*/
          v83 = sub_3684BC(v101); /*0x247420*/
        if ( v84 == 1 && (v85 = sub_9FAE4(v83), (v83 = sub_A1950(v85)) != 0) ) /*0x247434*/
        {
          v86 = netht_ctx_get_instance_576(v83); /*0x247438*/
          v87 = 0; /*0x24743c*/
        }
        else
        {
          v86 = netht_ctx_get_instance_576(v83); /*0x247444*/
          v87 = -1; /*0x247448*/
        }
        *(_DWORD *)(v86 + 392) = v87; /*0x24744c*/
        v88 = netht_ctx_get_instance_576(v86); /*0x247450*/
        result = sub_265BA0(src, v88); /*0x247458*/
        goto LABEL_188; /*0x247458*/
      case 17: /*0x2465dc*/
        if ( !a1 ) /*0x2467b4*/
        {
          strcpy(v107, "0"); /*0x246d30*/
          v63 = strlen(v107); /*0x246d34*/
          result = (__int64 *)sub_1FDD0((int)&v98, v107, v63); /*0x246d44*/
          goto LABEL_195; /*0x246d48*/
        }
        v21 = (*(__int64 (__fastcall **)(char *))(qword_4C2298 + 152))(a1); /*0x2467cc*/
        v22 = sub_240D28(v107, a1, v21); /*0x2467dc*/
        v23 = sub_305D9C(v22); /*0x2467e0*/
        v24 = sub_307FEC(v23, v107); /*0x2467e8*/
        result = sub_364088(src, v24); /*0x2467f0*/
LABEL_188:
        if ( (v98 & 1) != 0 ) /*0x247460*/
        {
          *v99 = 0; /*0x247470*/
          *((_QWORD *)&v98 + 1) = 0; /*0x247478*/
          if ( (v98 & 1) != 0 ) /*0x24747c*/
          {
            result = (__int64 *)sub_3684BC(v99); /*0x247484*/
            *(_QWORD *)&v98 = 0; /*0x247488*/
          }
        }
        else
        {
          LOWORD(v98) = 0; /*0x247464*/
        }
        v99 = v101; /*0x247498*/
        v98 = *(_OWORD *)src; /*0x24749c*/
        if ( (v107[0] & 1) != 0 ) /*0x2474a0*/
          goto LABEL_193; /*0x2474a0*/
        goto LABEL_195; /*0x2474a0*/
      case 18: /*0x2465dc*/
        if ( !a1 ) /*0x246a90*/
          goto LABEL_195; /*0x246a90*/
        memset(v107, 0, sizeof(v107)); /*0x246a98*/
        v108 = 0; /*0x246a9c*/
        v45 = strlen(a1); /*0x246aa0*/
        v46 = v45; /*0x246aa4*/
        if ( v45 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x246aac*/
          sub_1EA30(v107); /*0x24751c*/
        if ( v45 >= 0x17 ) /*0x246ab4*/
        {
          v67 = (v45 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x246ddc*/
          v47 = (char *)sub_368454(v67); /*0x246de8*/
          *(_QWORD *)&v107[8] = v46; /*0x246df0*/
          v108 = (__int64)v47; /*0x246df0*/
          *(_QWORD *)v107 = v67 | 1; /*0x246df4*/
        }
        else
        {
          v47 = &v107[1]; /*0x246ac0*/
          v107[0] = 2 * v45; /*0x246ac4*/
          if ( !v45 ) /*0x246ac8*/
            goto LABEL_102; /*0x246ac8*/
        }
        v45 = (size_t)memcpy(v47, a1, v46); /*0x246e04*/
LABEL_102:
        v47[v46] = 0; /*0x246e08*/
        v68 = sub_2343D8(v45); /*0x246e0c*/
        result = (__int64 *)sub_23446C(v68, v107); /*0x246e14*/
        if ( (v107[0] & 1) == 0 ) /*0x246e1c*/
          goto LABEL_195; /*0x246e1c*/
LABEL_193:
        v66 = v108; /*0x2474a4*/
        goto LABEL_194; /*0x2474a4*/
      case 19: /*0x2465dc*/
        if ( !a1 ) /*0x246b70*/
          goto LABEL_195; /*0x246b70*/
        memset(src, 0, sizeof(src)); /*0x246b78*/
        v101 = 0; /*0x246b7c*/
        v56 = strlen(a1); /*0x246b80*/
        v57 = v56; /*0x246b84*/
        if ( v56 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x246b8c*/
          sub_1EA30(src); /*0x247534*/
        if ( v56 >= 0x17 ) /*0x246b94*/
        {
          v78 = (v56 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x24722c*/
          v58 = (char *)sub_368454(v78); /*0x247238*/
          *(_QWORD *)&src[8] = v57; /*0x247240*/
          v101 = v58; /*0x247240*/
          *(_QWORD *)src = v78 | 1; /*0x247244*/
        }
        else
        {
          v58 = &src[1]; /*0x246ba0*/
          src[0] = 2 * v56; /*0x246ba4*/
          if ( !v56 ) /*0x246ba8*/
            goto LABEL_154; /*0x246ba8*/
        }
        memcpy(v58, a1, v57); /*0x247254*/
LABEL_154:
        v58[v57] = 0; /*0x247258*/
        strcpy((char *)v102, "l}lu{fkh{h"); /*0x247264*/
        for ( k = 0; k != 10; ++k ) /*0x247294*/
          *((_BYTE *)v102 + k) -= 7; /*0x2472b4*/
        sub_1E300(v97, v102, v96); /*0x2472d0*/
        if ( (src[0] & 1) != 0 ) /*0x2472ec*/
          LODWORD(v80) = (_DWORD)v101; /*0x2472ec*/
        else
          v80 = &src[1]; /*0x2472ec*/
        if ( (src[0] & 1) != 0 ) /*0x2472f0*/
          v81 = *(_QWORD *)&src[8]; /*0x2472f0*/
        else
          v81 = (unsigned __int64)(unsigned __int8)src[0] >> 1; /*0x2472f0*/
        sub_1F560(v95, (int)v80, v81); /*0x2472fc*/
        sub_1E338((__int64)v107, 0x270Fu, (__int64)v97, (__int64)v95, 0, 1); /*0x247318*/
        result = (__int64 *)sub_2F3308((__int64)v107); /*0x247324*/
        if ( v117 >= 0x40u ) /*0x247330*/
          result = (__int64 *)sub_1F748(&v116); /*0x247338*/
        if ( v115 >= 0x40u ) /*0x247348*/
          result = (__int64 *)sub_1F748(v114); /*0x247350*/
        if ( v113 >= 0x40u ) /*0x24735c*/
          result = (__int64 *)sub_1F748(&v112); /*0x247364*/
        if ( v111 >= 0x40u ) /*0x247374*/
          result = (__int64 *)sub_1F748(&v110); /*0x24737c*/
        if ( HIBYTE(v109) >= 0x40u ) /*0x247388*/
          result = (__int64 *)sub_1F748(&v107[8]); /*0x247390*/
        if ( v95[23] >= 0x40u ) /*0x24739c*/
          result = (__int64 *)sub_1F748(v95); /*0x2473a4*/
        if ( v97[23] < 0x40u ) /*0x2473b0*/
          goto LABEL_177; /*0x2473b0*/
        v73 = v97; /*0x2473b4*/
        goto LABEL_176; /*0x2473b4*/
      case 20: /*0x2465dc*/
        v59 = sub_2A205C(result); /*0x246be0*/
        result = (*(_QWORD *(__fastcall **)(char *__return_ptr))(*(_QWORD *)v59 + 16LL))(v107); /*0x246bf0*/
        goto LABEL_81; /*0x246bf4*/
      case 21: /*0x2465dc*/
        if ( !a1 ) /*0x246ad0*/
          goto LABEL_195; /*0x246ad0*/
        memset(src, 0, sizeof(src)); /*0x246ad8*/
        v101 = 0; /*0x246adc*/
        v48 = strlen(a1); /*0x246ae0*/
        v49 = v48; /*0x246ae4*/
        if ( v48 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x246aec*/
          sub_1EA30(src); /*0x247524*/
        if ( v48 >= 0x17 ) /*0x246af4*/
        {
          v69 = (v48 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x246e28*/
          v50 = (char *)sub_368454(v69); /*0x246e34*/
          *(_QWORD *)&src[8] = v49; /*0x246e3c*/
          v101 = v50; /*0x246e3c*/
          *(_QWORD *)src = v69 | 1; /*0x246e40*/
        }
        else
        {
          v50 = &src[1]; /*0x246b00*/
          src[0] = 2 * v48; /*0x246b04*/
          if ( !v48 ) /*0x246b08*/
            goto LABEL_106; /*0x246b08*/
        }
        memcpy(v50, a1, v49); /*0x246e50*/
LABEL_106:
        v50[v49] = 0; /*0x246e54*/
        v102[0] = 122; /*0x246e5c*/
        v102[1] = 287507213; /*0x246e74*/
        v70 = 0; /*0x246e84*/
        v102[2] = -269936623; /*0x246ec0*/
        v102[3] = -253168675; /*0x246f04*/
        v103 = -31; /*0x246f50*/
        v104 = -18; /*0x246f60*/
        v105 = -26; /*0x246f70*/
        v106 = 0; /*0x246f74*/
        do /*0x246f94*/
        {
          *((_BYTE *)&v102[1] + v70) ^= (_BYTE)v70 + LOBYTE(v102[0]); /*0x246f88*/
          ++v70; /*0x246f8c*/
        }
        while ( v70 != 15 ); /*0x246f94*/
        v106 = 0; /*0x246f98*/
        sub_1E300(v94, &v102[1], v96); /*0x246fa4*/
        if ( (src[0] & 1) != 0 ) /*0x246fc0*/
          LODWORD(v71) = (_DWORD)v101; /*0x246fc0*/
        else
          v71 = &src[1]; /*0x246fc0*/
        if ( (src[0] & 1) != 0 ) /*0x246fc4*/
          v72 = *(_QWORD *)&src[8]; /*0x246fc4*/
        else
          v72 = (unsigned __int64)(unsigned __int8)src[0] >> 1; /*0x246fc4*/
        sub_1F560(v93, (int)v71, v72); /*0x246fd0*/
        sub_1E338((__int64)v107, 0x270Fu, (__int64)v94, (__int64)v93, 0, 1); /*0x246fec*/
        result = (__int64 *)sub_2F3308((__int64)v107); /*0x246ff8*/
        if ( v117 >= 0x40u ) /*0x247004*/
          result = (__int64 *)sub_1F748(&v116); /*0x24700c*/
        if ( v115 >= 0x40u ) /*0x24701c*/
          result = (__int64 *)sub_1F748(v114); /*0x247024*/
        if ( v113 >= 0x40u ) /*0x247030*/
          result = (__int64 *)sub_1F748(&v112); /*0x247038*/
        if ( v111 >= 0x40u ) /*0x247048*/
          result = (__int64 *)sub_1F748(&v110); /*0x247050*/
        if ( HIBYTE(v109) >= 0x40u ) /*0x24705c*/
          result = (__int64 *)sub_1F748(&v107[8]); /*0x247064*/
        if ( v93[23] >= 0x40u ) /*0x247070*/
          result = (__int64 *)sub_1F748(v93); /*0x247078*/
        if ( v94[23] < 0x40u ) /*0x247084*/
          goto LABEL_177; /*0x247084*/
        v73 = v94; /*0x247088*/
        goto LABEL_176; /*0x24708c*/
      case 22: /*0x2465dc*/
        if ( !a1 ) /*0x246b10*/
          goto LABEL_195; /*0x246b10*/
        memset(src, 0, sizeof(src)); /*0x246b18*/
        v101 = 0; /*0x246b1c*/
        v51 = strlen(a1); /*0x246b20*/
        v52 = v51; /*0x246b24*/
        if ( v51 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x246b2c*/
          sub_1EA30(src); /*0x24752c*/
        if ( v51 >= 0x17 ) /*0x246b34*/
        {
          v74 = (v51 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x247094*/
          v53 = (char *)sub_368454(v74); /*0x2470a0*/
          *(_QWORD *)&src[8] = v52; /*0x2470a8*/
          v101 = v53; /*0x2470a8*/
          *(_QWORD *)src = v74 | 1; /*0x2470ac*/
        }
        else
        {
          v53 = &src[1]; /*0x246b40*/
          src[0] = 2 * v51; /*0x246b44*/
          if ( !v51 ) /*0x246b48*/
            goto LABEL_130; /*0x246b48*/
        }
        memcpy(v53, a1, v52); /*0x2470bc*/
LABEL_130:
        v53[v52] = 0; /*0x2470c0*/
        v102[0] = 27; /*0x2470cc*/
        strcpy((char *)&v102[1], "p~b~m~uo"); /*0x2470d4*/
        for ( m = 0; m != 8; ++m ) /*0x2470fc*/
          *((_BYTE *)&v102[1] + m) ^= LOBYTE(v102[0]); /*0x247120*/
        LOBYTE(v102[3]) = 0; /*0x247130*/
        sub_1E300(v92, &v102[1], v96); /*0x24713c*/
        if ( (src[0] & 1) != 0 ) /*0x247158*/
          LODWORD(v76) = (_DWORD)v101; /*0x247158*/
        else
          v76 = &src[1]; /*0x247158*/
        if ( (src[0] & 1) != 0 ) /*0x24715c*/
          v77 = *(_QWORD *)&src[8]; /*0x24715c*/
        else
          v77 = (unsigned __int64)(unsigned __int8)src[0] >> 1; /*0x24715c*/
        sub_1F560(v91, (int)v76, v77); /*0x247168*/
        sub_1E338((__int64)v107, 0xFA2u, (__int64)v92, (__int64)v91, 0, 1); /*0x247184*/
        result = (__int64 *)sub_2F3308((__int64)v107); /*0x247190*/
        if ( v117 >= 0x40u ) /*0x24719c*/
          result = (__int64 *)sub_1F748(&v116); /*0x2471a4*/
        if ( v115 >= 0x40u ) /*0x2471b4*/
          result = (__int64 *)sub_1F748(v114); /*0x2471bc*/
        if ( v113 >= 0x40u ) /*0x2471c8*/
          result = (__int64 *)sub_1F748(&v112); /*0x2471d0*/
        if ( v111 >= 0x40u ) /*0x2471e0*/
          result = (__int64 *)sub_1F748(&v110); /*0x2471e8*/
        if ( HIBYTE(v109) >= 0x40u ) /*0x2471f4*/
          result = (__int64 *)sub_1F748(&v107[8]); /*0x2471fc*/
        if ( v91[23] >= 0x40u ) /*0x247208*/
          result = (__int64 *)sub_1F748(v91); /*0x247210*/
        if ( v92[23] >= 0x40u ) /*0x24721c*/
        {
          v73 = v92; /*0x247220*/
LABEL_176:
          result = (__int64 *)sub_1F748(v73); /*0x2473b8*/
        }
LABEL_177:
        if ( (src[0] & 1) != 0 ) /*0x2473c0*/
        {
          v66 = (__int64)v101; /*0x2473c4*/
LABEL_194:
          result = (__int64 *)sub_3684BC(v66); /*0x2474a8*/
        }
        goto LABEL_195; /*0x2474a8*/
      default:
        *(_DWORD *)v107 = 98; /*0x246704*/
        v107[4] = 23; /*0x24670c*/
        v107[5] = 12; /*0x246710*/
        v107[6] = 17; /*0x246718*/
        v107[7] = 23; /*0x24671c*/
        *(_QWORD *)&v107[8] = 0x42060716100D1212LL; /*0x246720*/
        v108 = 0x16110717130710LL; /*0x246754*/
        for ( n = 4; n != 23; ++n ) /*0x246770*/
          v107[n] ^= v107[0]; /*0x246784*/
        HIBYTE(v108) = 0; /*0x246798*/
        v20 = strlen(&v107[4]); /*0x24679c*/
        result = (__int64 *)sub_1FDD0((int)&v98, &v107[4], v20); /*0x2467ac*/
        goto LABEL_195; /*0x2467b0*/
    }
  }
  if ( a1 ) /*0x2464c4*/
  {
    memset(v107, 0, sizeof(v107)); /*0x2464cc*/
    v108 = 0; /*0x2464d0*/
    v6 = strlen(a1); /*0x2464d4*/
    v7 = v6; /*0x2464d8*/
    if ( v6 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x2464e0*/
      sub_1EA30(v107); /*0x24750c*/
    if ( v6 >= 0x17 ) /*0x2464e8*/
    {
      v13 = (v6 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x2465fc*/
      v8 = (char *)sub_368454(v13); /*0x246608*/
      *(_QWORD *)&v107[8] = v7; /*0x246610*/
      v108 = (__int64)v8; /*0x246610*/
      *(_QWORD *)v107 = v13 | 1; /*0x246614*/
    }
    else
    {
      v8 = &v107[1]; /*0x2464f4*/
      v107[0] = 2 * v6; /*0x2464f8*/
      if ( !v6 ) /*0x2464fc*/
      {
LABEL_19:
        v8[v7] = 0; /*0x246628*/
        v102[0] = 58; /*0x246630*/
        strcpy((char *)&v102[1], "LS_Me"); /*0x246638*/
        for ( ii = 0; ii != 5; ++ii ) /*0x246660*/
          *((_BYTE *)&v102[1] + ii) ^= LOBYTE(v102[0]); /*0x246678*/
        BYTE1(v102[2]) = 0; /*0x246688*/
        v15 = sub_5956C(src, &v102[1], v107); /*0x246694*/
        v16 = netht_ctx_get_instance_576(v15); /*0x246698*/
        result = (__int64 *)netht_named_key_record(v16, src); /*0x2466a0*/
        v17 = v98; /*0x2466a8*/
        v18 = src[0]; /*0x2466ac*/
        v98 = 0u; /*0x2466b0*/
        a3[2] = v99; /*0x2466b4*/
        *(_OWORD *)a3 = v17; /*0x2466b8*/
        v99 = 0; /*0x2466bc*/
        if ( (v18 & 1) != 0 ) /*0x2466c0*/
          result = (__int64 *)sub_3684BC(v101); /*0x2466c8*/
LABEL_23:
        if ( (v107[0] & 1) != 0 ) /*0x2466d0*/
          result = (__int64 *)sub_3684BC(v108); /*0x2466d8*/
        goto LABEL_196; /*0x2466dc*/
      }
    }
    memcpy(v8, a1, v7); /*0x246624*/
    goto LABEL_19; /*0x246624*/
  }
LABEL_195:
  v89 = v99; /*0x2474ac*/
  v90 = v98; /*0x2474b0*/
  v98 = 0u; /*0x2474b4*/
  v99 = 0; /*0x2474b8*/
  a3[2] = v89; /*0x2474bc*/
  *(_OWORD *)a3 = v90; /*0x2474c0*/
LABEL_196:
  if ( (v98 & 1) != 0 ) /*0x2474c8*/
    return (__int64 *)sub_3684BC(v99); /*0x2474d0*/
  return result; /*0x247500*/
}