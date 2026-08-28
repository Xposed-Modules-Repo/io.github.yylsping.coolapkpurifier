__int64 __fastcall sub_242788(__int64 a1, __int64 a2, __int64 a3, __int64 a4, __int64 a5, __int64 a6)
{
  const char *v11; // x22
  __int64 v12; // x9
  __int64 v13; // x8
  __int64 v14; // x8
  __int64 v15; // x8
  unsigned __int64 v16; // x8
  void (*v17)(void); // x8
  __int64 v18; // x1
  __int64 v19; // x8
  __int64 v20; // x23
  __int64 result; // x0
  _QWORD v22[2]; // [xsp+20h] [xbp-180h] BYREF
  __int128 v23; // [xsp+30h] [xbp-170h] BYREF
  _BYTE *v24; // [xsp+40h] [xbp-160h]
  int v25; // [xsp+48h] [xbp-158h]
  __int128 v26; // [xsp+50h] [xbp-150h]
  _BYTE *v27; // [xsp+60h] [xbp-140h]
  _QWORD *v28; // [xsp+68h] [xbp-138h] BYREF
  _QWORD v29[2]; // [xsp+70h] [xbp-130h] BYREF
  __int128 v30; // [xsp+80h] [xbp-120h]
  _BYTE *v31; // [xsp+90h] [xbp-110h]
  __int128 v32; // [xsp+98h] [xbp-108h] BYREF
  __int64 v33; // [xsp+A8h] [xbp-F8h]
  _BYTE v34[32]; // [xsp+B0h] [xbp-F0h] BYREF
  _BYTE v35[32]; // [xsp+D0h] [xbp-D0h] BYREF
  _BYTE *v36; // [xsp+F0h] [xbp-B0h]
  __int128 v37; // [xsp+108h] [xbp-98h] BYREF
  __int64 v38; // [xsp+118h] [xbp-88h]
  char v39; // [xsp+120h] [xbp-80h]
  char v40; // [xsp+121h] [xbp-7Fh]
  char v41; // [xsp+122h] [xbp-7Eh]
  char v42; // [xsp+123h] [xbp-7Dh]
  char v43; // [xsp+124h] [xbp-7Ch]
  char v44; // [xsp+125h] [xbp-7Bh]
  char v45[10]; // [xsp+126h] [xbp-7Ah] BYREF
  __int128 v46; // [xsp+130h] [xbp-70h] BYREF
  __int64 v47; // [xsp+140h] [xbp-60h]

  _ReadStatusReg(TPIDR_EL0); /*0x2427a8*/
  v11 = (const char *)(*(__int64 (__fastcall **)(__int64, __int64, _QWORD))(*(_QWORD *)a1 + 1352LL))(a1, a4, 0); /*0x2427e4*/
  v25 = 1; /*0x2427e8*/
  v23 = 0u; /*0x2427f0*/
  v24 = 0; /*0x2427f4*/
  v26 = 0u; /*0x2427f8*/
  v29[1] = 0; /*0x242800*/
  v30 = 0u; /*0x242800*/
  v29[0] = 0; /*0x242804*/
  v27 = 0; /*0x242808*/
  v28 = v29; /*0x242808*/
  v31 = 0; /*0x24280c*/
  if ( !a6 ) /*0x242814*/
    goto LABEL_40; /*0x242814*/
  sub_1AE7AC(v34, a6, 0); /*0x242824*/
  *(_QWORD *)&v37 = 0x2C283F290000005ALL; /*0x242838*/
  DWORD2(v37) = 588130367; /*0x242868*/
  v12 = 4; /*0x242880*/
  WORD6(v37) = 16170; /*0x242884*/
  BYTE14(v37) = 0; /*0x24288c*/
  do /*0x2428a8*/
    *((_BYTE *)&v37 + v12++) ^= v37; /*0x24289c*/
  while ( v12 != 14 ); /*0x2428a8*/
  BYTE14(v37) = 0; /*0x2428ac*/
  v25 = sub_2801C(v34, (char *)&v37 + 4); /*0x2428bc*/
  LODWORD(v46) = 2; /*0x2428c0*/
  v13 = 0; /*0x2428cc*/
  strcpy((char *)&v46 + 4, "akekhbd"); /*0x2428d4*/
  do /*0x242974*/
  {
    *((_BYTE *)&v46 + v13 + 4) ^= (_BYTE)v13 + (_BYTE)v46; /*0x242968*/
    ++v13; /*0x24296c*/
  }
  while ( v13 != 7 ); /*0x242974*/
  BYTE11(v46) = 0; /*0x242978*/
  sub_280C4(&v37, v34); /*0x242984*/
  if ( (v26 & 1) != 0 ) /*0x24298c*/
  {
    *v27 = 0; /*0x24299c*/
    *((_QWORD *)&v26 + 1) = 0; /*0x2429a4*/
    if ( (v26 & 1) != 0 ) /*0x2429a8*/
    {
      sub_3684BC(v27); /*0x2429b0*/
      *(_QWORD *)&v26 = 0; /*0x2429b4*/
    }
  }
  else
  {
    LOWORD(v26) = 0; /*0x242990*/
  }
  v14 = 0; /*0x2429c4*/
  v27 = (_BYTE *)v38; /*0x2429c8*/
  v26 = v37; /*0x2429d0*/
  qmemcpy(&v46, "mgskQk", 6); /*0x2429d4*/
  WORD3(v46) = 127; /*0x2429fc*/
  do /*0x242a1c*/
    *((_BYTE *)&v46 + v14++) -= 6; /*0x242a10*/
  while ( v14 != 7 ); /*0x242a1c*/
  sub_280C4(&v37, v34); /*0x242a2c*/
  if ( (v23 & 1) != 0 ) /*0x242a34*/
  {
    *v24 = 0; /*0x242a44*/
    *((_QWORD *)&v23 + 1) = 0; /*0x242a4c*/
    if ( (v23 & 1) != 0 ) /*0x242a50*/
    {
      sub_3684BC(v24); /*0x242a58*/
      *(_QWORD *)&v23 = 0; /*0x242a5c*/
    }
  }
  else
  {
    LOWORD(v23) = 0; /*0x242a38*/
  }
  v24 = (_BYTE *)v38; /*0x242a70*/
  v23 = v37; /*0x242a74*/
  *(_QWORD *)&v37 = 0x7D43535200000035LL; /*0x242a78*/
  v15 = 0; /*0x242a9c*/
  *((_QWORD *)&v37 + 1) = 0x214B5F795D494E41LL; /*0x242adc*/
  v38 = 0x672630242E0F6B69LL; /*0x242b64*/
  v39 = 37; /*0x242bf8*/
  v40 = 43; /*0x242c08*/
  v41 = 37; /*0x242c1c*/
  v42 = 43; /*0x242c2c*/
  v43 = 98; /*0x242c3c*/
  v44 = 29; /*0x242c50*/
  strcpy(v45, ";\"8<4o"); /*0x242c60*/
  do /*0x242ce4*/
  {
    *((_BYTE *)&v37 + v15 + 4) ^= (_BYTE)v15 + (_BYTE)v37; /*0x242cd8*/
    ++v15; /*0x242cdc*/
  }
  while ( v15 != 32 ); /*0x242ce4*/
  v45[6] = 0; /*0x242ce8*/
  sub_541F0((__int64 *)&v46, (int)v34, (char *)&v37 + 4); /*0x242cf4*/
  if ( (v46 & 1) != 0 ) /*0x242d08*/
    v16 = *((_QWORD *)&v46 + 1); /*0x242d08*/
  else
    v16 = (unsigned __int64)(unsigned __int8)v46 >> 1; /*0x242d08*/
  if ( v16 ) /*0x242d0c*/
  {
    *((_QWORD *)&v37 + 1) = 0; /*0x242d18*/
    v38 = 0; /*0x242d18*/
    *(_QWORD *)&v37 = (char *)&v37 + 8; /*0x242d1c*/
    v36 = 0; /*0x242d20*/
    sub_A1B98(v22, &v46, v35, 1, 0); /*0x242d3c*/
    if ( v35 == v36 ) /*0x242d48*/
    {
      v17 = *(void (**)(void))(*(_QWORD *)v36 + 32LL); /*0x242d60*/
    }
    else
    {
      if ( !v36 ) /*0x242d4c*/
      {
LABEL_27:
        sub_243F10(&v32, v22); /*0x242d68*/
        sub_3A838(&v37, *((_QWORD *)&v37 + 1)); /*0x242d80*/
        v18 = *((_QWORD *)&v32 + 1); /*0x242d84*/
        v37 = v32; /*0x242d8c*/
        v38 = v33; /*0x242d90*/
        if ( v33 ) /*0x242d94*/
        {
          *(_QWORD *)(*((_QWORD *)&v32 + 1) + 16LL) = (char *)&v37 + 8; /*0x242d9c*/
          v18 = 0; /*0x242da0*/
          *(_QWORD *)&v32 = (char *)&v32 + 8; /*0x242da4*/
          *((_QWORD *)&v32 + 1) = 0; /*0x242da8*/
          v33 = 0; /*0x242da8*/
        }
        else
        {
          *(_QWORD *)&v37 = (char *)&v37 + 8; /*0x242db0*/
        }
        sub_3A838(&v32, v18); /*0x242db8*/
        sub_24438C(&v28, v37, (char *)&v37 + 8); /*0x242dcc*/
        sub_7B92C(v22); /*0x242dd8*/
        sub_3A838(&v37, *((_QWORD *)&v37 + 1)); /*0x242de4*/
        goto LABEL_31; /*0x242de4*/
      }
      v17 = *(void (**)(void))(*(_QWORD *)v36 + 40LL); /*0x242d54*/
    }
    v17(); /*0x242d64*/
    goto LABEL_27; /*0x242d64*/
  }
LABEL_31:
  v19 = 0; /*0x242de8*/
  *(_QWORD *)&v32 = 0x24233F3800000050LL; /*0x242e08*/
  BYTE8(v32) = 0; /*0x242e1c*/
  do /*0x242e38*/
    *((_BYTE *)&v32 + v19++ + 4) ^= v32; /*0x242e2c*/
  while ( v19 != 4 ); /*0x242e38*/
  BYTE8(v32) = 0; /*0x242e3c*/
  sub_280C4(&v37, v34); /*0x242e48*/
  if ( (v30 & 1) != 0 ) /*0x242e50*/
  {
    *v31 = 0; /*0x242e60*/
    *((_QWORD *)&v30 + 1) = 0; /*0x242e68*/
    if ( (v30 & 1) != 0 ) /*0x242e6c*/
    {
      sub_3684BC(v31); /*0x242e74*/
      *(_QWORD *)&v30 = 0; /*0x242e78*/
    }
  }
  else
  {
    LOWORD(v30) = 0; /*0x242e54*/
  }
  v31 = (_BYTE *)v38; /*0x242e88*/
  v30 = v37; /*0x242e8c*/
  if ( (v46 & 1) != 0 ) /*0x242e90*/
    sub_3684BC(v47); /*0x242e98*/
  sub_1B0760(v34); /*0x242ea0*/
LABEL_40:
  v20 = (*(__int64 (__fastcall **)(__int64, __int64))(*(_QWORD *)a1 + 168LL))(a1, a2); /*0x242ea4*/
  if ( (*(unsigned __int8 (__fastcall **)(__int64))(*(_QWORD *)a1 + 1824LL))(a1) ) /*0x242ec8*/
  {
    (*(void (__fastcall **)(__int64))(*(_QWORD *)a1 + 136LL))(a1); /*0x242ee0*/
    v20 = 0; /*0x242ee4*/
  }
  sub_244778(v11, (__int64)&v23, a5, 0, v20); /*0x242efc*/
  (*(void (__fastcall **)(__int64, __int64, const char *))(*(_QWORD *)a1 + 1360LL))(a1, a4, v11); /*0x242f14*/
  if ( (v30 & 1) != 0 ) /*0x242f1c*/
    sub_3684BC(v31); /*0x242f24*/
  result = sub_3A838(&v28, v29[0]); /*0x242f34*/
  if ( (v26 & 1) != 0 ) /*0x242f3c*/
    result = sub_3684BC(v27); /*0x242f44*/
  if ( (v23 & 1) != 0 ) /*0x242f4c*/
    return sub_3684BC(v24); /*0x242f54*/
  return result; /*0x242f84*/
}