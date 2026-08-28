__int64 netht_config_field_updater()
{
  __int64 v0; // x0
  __int64 v1; // x0
  __int64 v2; // x0
  __int64 v3; // x28
  __int64 v4; // x12
  __int64 v5; // x26
  int v6; // w20
  unsigned __int64 v8; // x8
  __int64 v9; // x0
  unsigned __int64 v10; // x1
  int v11; // w20
  __int64 v12; // x0
  __int64 k; // x8
  __int64 v14; // x19
  __int64 v15; // x0
  __int64 v16; // x0
  __int64 v17; // x0
  __int64 v18; // x20
  __int64 v19; // x23
  __int64 v20; // x0
  time_t v21; // x28
  __int64 v22; // x20
  time_t v23; // x28
  unsigned int v24; // w19
  __int64 v25; // x8
  unsigned __int64 v26; // x8
  __int64 v27; // x1
  size_t v28; // x2
  unsigned __int8 v29; // w19
  int v30; // w22
  int v31; // w25
  char v32; // w23
  char v33; // w19
  __int64 v34; // x0
  __int64 v35; // x0
  __int64 (__fastcall *v36)(_QWORD); // x19
  __int64 v37; // x0
  __int64 v38; // x0
  __int64 v39; // x0
  __int64 v40; // x0
  __int64 v41; // x0
  __int64 m; // x8
  size_t v43; // x0
  size_t v44; // x23
  char *v45; // x19
  unsigned __int64 v46; // x22
  unsigned __int64 v47; // x9
  __int64 v48; // x0
  __int64 v49; // x19
  unsigned __int64 v50; // x2
  char *v51; // x1
  char *v52; // x0
  unsigned __int64 v53; // x2
  __int64 v54; // x19
  __int64 v55; // x0
  unsigned int *v56; // x22
  __int64 v57; // x25
  __int64 v58; // x0
  __int64 i; // x8
  __int64 v60; // x0
  __int64 j; // x8
  unsigned int v62; // w8
  unsigned __int64 v63; // x8
  __int64 v64; // x1
  size_t v65; // x2
  char v66; // w19
  unsigned __int64 v67; // x8
  __int64 v68; // x1
  size_t v69; // x2
  __int64 n; // x8
  size_t v71; // x0
  size_t v72; // x23
  char *v73; // x19
  const char *v74; // x19
  const char *v75; // x2
  const char *v76; // x1
  __int64 v77; // x19
  unsigned __int64 v78; // x8
  __int64 v79; // x1
  unsigned __int64 v80; // x8
  unsigned __int64 v81; // x8
  __int64 v82; // x28
  unsigned __int64 v83; // x23
  __int64 v84; // x10
  __int64 v85; // x0
  __int64 v86; // x12
  __int64 v87; // x9
  unsigned __int64 v88; // x11
  _QWORD *v89; // x12
  _QWORD *v90; // x13
  _QWORD *v91; // x10
  __int64 v92; // x12
  __int64 v93; // x14
  char v94; // w23
  __int64 v95; // x19
  unsigned __int64 *v96; // x8
  unsigned __int64 v97; // x9
  __int64 v98; // x20
  __int64 v99; // x26
  unsigned __int64 v100; // x28
  int v101; // [xsp+0h] [xbp-4E0h]
  __int64 v102; // [xsp+80h] [xbp-460h]
  int v103; // [xsp+8Ch] [xbp-454h]
  __int64 v104; // [xsp+B8h] [xbp-428h]
  char v105; // [xsp+D4h] [xbp-40Ch]
  __int64 v106; // [xsp+E0h] [xbp-400h]
  _BYTE *v107; // [xsp+E8h] [xbp-3F8h]
  unsigned int v108; // [xsp+F0h] [xbp-3F0h]
  unsigned int *v109; // [xsp+F0h] [xbp-3F0h]
  char v110[8]; // [xsp+F8h] [xbp-3E8h] BYREF
  _BYTE v111[24]; // [xsp+100h] [xbp-3E0h] BYREF
  char v112[8]; // [xsp+118h] [xbp-3C8h] BYREF
  _BYTE v113[24]; // [xsp+120h] [xbp-3C0h] BYREF
  _BYTE v114[24]; // [xsp+138h] [xbp-3A8h] BYREF
  _BYTE v115[24]; // [xsp+150h] [xbp-390h] BYREF
  char v116[8]; // [xsp+168h] [xbp-378h] BYREF
  _BYTE v117[24]; // [xsp+170h] [xbp-370h] BYREF
  _BYTE v118[23]; // [xsp+188h] [xbp-358h] BYREF
  unsigned __int8 v119; // [xsp+19Fh] [xbp-341h]
  _QWORD v120[3]; // [xsp+1A0h] [xbp-340h] BYREF
  __int64 v121; // [xsp+1B8h] [xbp-328h] BYREF
  __int64 v122; // [xsp+1C0h] [xbp-320h]
  int v123; // [xsp+1D0h] [xbp-310h] BYREF
  _BYTE v124[24]; // [xsp+1D8h] [xbp-308h] BYREF
  _BYTE v125[24]; // [xsp+1F0h] [xbp-2F0h] BYREF
  _BYTE v126[24]; // [xsp+208h] [xbp-2D8h] BYREF
  _BYTE v127[24]; // [xsp+220h] [xbp-2C0h] BYREF
  _BYTE v128[28]; // [xsp+238h] [xbp-2A8h] BYREF
  unsigned __int8 v129; // [xsp+254h] [xbp-28Ch]
  __int64 s; // [xsp+270h] [xbp-270h] BYREF
  size_t v131; // [xsp+278h] [xbp-268h]
  char *v132; // [xsp+280h] [xbp-260h]
  __int64 v133; // [xsp+288h] [xbp-258h]
  __int64 v134; // [xsp+290h] [xbp-250h]
  char v135; // [xsp+298h] [xbp-248h]
  __int64 v136; // [xsp+2A0h] [xbp-240h]
  __int64 v137; // [xsp+2A8h] [xbp-238h]
  char v138; // [xsp+2B0h] [xbp-230h]
  time_t v139; // [xsp+2B8h] [xbp-228h] BYREF
  __int64 v140; // [xsp+2C0h] [xbp-220h]
  time_t v141; // [xsp+2C8h] [xbp-218h] BYREF
  char v142; // [xsp+2D0h] [xbp-210h]
  __int64 v143; // [xsp+2D8h] [xbp-208h]
  int v144; // [xsp+2F0h] [xbp-1F0h]
  char v145[20]; // [xsp+2F4h] [xbp-1ECh] BYREF
  _QWORD v146[2]; // [xsp+308h] [xbp-1D8h] BYREF
  __int64 v147; // [xsp+318h] [xbp-1C8h]
  _QWORD v148[2]; // [xsp+320h] [xbp-1C0h] BYREF
  char v149; // [xsp+330h] [xbp-1B0h]
  __int64 v150; // [xsp+338h] [xbp-1A8h]
  __int64 v151; // [xsp+340h] [xbp-1A0h]
  char v152; // [xsp+348h] [xbp-198h]
  __int64 v153; // [xsp+350h] [xbp-190h]
  __int64 v154; // [xsp+358h] [xbp-188h]
  char v155; // [xsp+360h] [xbp-180h]
  char v156; // [xsp+368h] [xbp-178h]
  __int64 v157; // [xsp+370h] [xbp-170h]
  __int128 v158; // [xsp+408h] [xbp-D8h] BYREF
  __int128 v159; // [xsp+418h] [xbp-C8h]
  __int128 v160; // [xsp+428h] [xbp-B8h]
  __int128 v161; // [xsp+438h] [xbp-A8h]
  char v162[64]; // [xsp+448h] [xbp-98h] BYREF
  __int64 v163; // [xsp+488h] [xbp-58h]

  v163 = *(_QWORD *)(_ReadStatusReg(TPIDR_EL0) + 40); /*0x2a684*/
  v0 = sub_26CF8C(10); /*0x2a688*/
  if ( (v0 & 1) == 0 ) /*0x2a68c*/
  {
    v1 = sub_9FAE4(v0); /*0x2a690*/
    v2 = sub_9FB8C(v1); /*0x2a694*/
    v3 = sub_9B83C(v2, 203695656); /*0x2a6a4*/
    if ( *(_QWORD *)v3 != *(_QWORD *)(v3 + 8) ) /*0x2a6b0*/
    {
      sub_2BE378(&v121); /*0x2a6bc*/
      v5 = v121; /*0x2a6c0*/
      v4 = v122; /*0x2a6c0*/
      if ( v121 == v122 ) /*0x2a6c8*/
      {
        LOBYTE(v6) = 0; /*0x2a7b8*/
        goto LABEL_8; /*0x2a7b8*/
      }
      v104 = v3; /*0x2a728*/
      v102 = v122; /*0x2a758*/
      v6 = 0; /*0x2a768*/
      while ( 1 ) /*0x2b014*/
      {
        v56 = *(unsigned int **)v3; /*0x2b014*/
        v109 = *(unsigned int **)(v3 + 8); /*0x2b01c*/
        if ( *(unsigned int **)v3 == v109 ) /*0x2b020*/
          goto LABEL_190; /*0x2b020*/
        v57 = v5 + 49; /*0x2b024*/
        v103 = v6; /*0x2b02c*/
        v106 = v5 + 49; /*0x2b030*/
        v107 = (_BYTE *)(v5 + 48); /*0x2b030*/
        while ( 1 ) /*0x2b0e8*/
        {
          if ( (sub_2301B4(*v56) & 1) == 0 ) /*0x2b0f8*/
            goto LABEL_188; /*0x2b0f8*/
          v58 = v106; /*0x2b0fc*/
          if ( (*v107 & 1) != 0 ) /*0x2b104*/
            v58 = *(_QWORD *)(v5 + 64); /*0x2b108*/
          LODWORD(v146[0]) = 38; /*0x2b110*/
          strcpy((char *)v146 + 4, "\tBGRG\tGVV\t"); /*0x2b11c*/
          for ( i = 4; i != 14; *((_BYTE *)v146 + i++) ^= LOBYTE(v146[0]) ) /*0x2b150*/
            ; /*0x2b164*/
          BYTE6(v146[1]) = 0; /*0x2b174*/
          if ( (sub_2BDBB8(v58, (char *)v146 + 4) & 1) != 0 ) /*0x2b180*/
          {
            v60 = v106; /*0x2b184*/
            if ( (*v107 & 1) != 0 ) /*0x2b18c*/
              v60 = *(_QWORD *)(v5 + 64); /*0x2b190*/
            strcpy((char *)&s, "2eto"); /*0x2b198*/
            for ( j = 0; j != 4; *((_BYTE *)&s + j++) -= 4 ) /*0x2b1a8*/
              ; /*0x2b1c4*/
            if ( (sub_2BDB30(v60, &s) & 1) != 0 ) /*0x2b1dc*/
              goto LABEL_188; /*0x2b1dc*/
          }
          v62 = v56[1]; /*0x2b1e0*/
          if ( v62 != 1107622090 ) /*0x2b1f0*/
            break; /*0x2b1f0*/
          v138 = 0; /*0x2b344*/
          v136 = 0; /*0x2b348*/
          v137 = 0; /*0x2b348*/
          LOBYTE(v141) = 0; /*0x2b350*/
          v139 = 0; /*0x2b354*/
          v140 = 0; /*0x2b354*/
          v142 = 0; /*0x2b358*/
          v143 = 0; /*0x2b35c*/
          v134 = 0; /*0x2b360*/
          v133 = 0; /*0x2b364*/
          v132 = 0; /*0x2b368*/
          v131 = 0; /*0x2b36c*/
          v135 = 0; /*0x2b370*/
          s = 0; /*0x2b374*/
          if ( *((unsigned __int8 *)v56 + 39) >= 0x40u ) /*0x2b388*/
            v74 = (const char *)*((_QWORD *)v56 + 2); /*0x2b388*/
          else
            v74 = (const char *)(v56 + 4); /*0x2b388*/
          sub_2BBB8(v162); /*0x2b390*/
          memset(&v162[24], 0, 40); /*0x2b3a4*/
          v75 = &v74[strlen(v74)]; /*0x2b3ac*/
          v76 = v74; /*0x2b3b4*/
          v77 = v57; /*0x2b3b8*/
          sub_2BC14(v162, v76, v75); /*0x2b3bc*/
          v152 = 0; /*0x2b3c4*/
          v150 = 0; /*0x2b3c8*/
          v151 = 0; /*0x2b3c8*/
          v155 = 0; /*0x2b3d0*/
          v153 = 0; /*0x2b3d4*/
          v154 = 0; /*0x2b3d4*/
          v156 = 0; /*0x2b3d8*/
          v157 = 0; /*0x2b3dc*/
          v149 = 0; /*0x2b3e0*/
          v148[1] = 0; /*0x2b3e4*/
          v148[0] = 0; /*0x2b3e8*/
          v147 = 0; /*0x2b3ec*/
          v146[1] = 0; /*0x2b3f0*/
          v146[0] = 0; /*0x2b3f4*/
          v78 = *(unsigned __int8 *)(v5 + 48); /*0x2b3f8*/
          if ( (v78 & 1) != 0 ) /*0x2b408*/
            v79 = *(_QWORD *)(v5 + 64); /*0x2b408*/
          else
            v79 = v57; /*0x2b408*/
          if ( (v78 & 1) != 0 ) /*0x2b40c*/
            v80 = *(_QWORD *)(v5 + 56); /*0x2b40c*/
          else
            v80 = v78 >> 1; /*0x2b40c*/
          v105 = sub_34CDC(v162, v79, v79 + v80, v146, 0); /*0x2b424*/
          v81 = *(unsigned __int8 *)(v5 + 48); /*0x2b428*/
          v82 = v150; /*0x2b438*/
          if ( (v81 & 1) != 0 ) /*0x2b44c*/
          {
            v77 = *(_QWORD *)(v5 + 64); /*0x2b44c*/
            v83 = *(_QWORD *)(v5 + 56); /*0x2b450*/
          }
          else
          {
            v83 = v81 >> 1; /*0x2b450*/
          }
          sub_37024(&s, 0xAAAAAAAAAAAAAAABLL * ((__int64)(v146[1] - v146[0]) >> 3)); /*0x2b45c*/
          v84 = s; /*0x2b464*/
          if ( v131 == s ) /*0x2b470*/
          {
            v85 = v146[0]; /*0x2b474*/
          }
          else
          {
            v86 = v146[1]; /*0x2b47c*/
            v85 = v146[0]; /*0x2b480*/
            v87 = 0; /*0x2b488*/
            v88 = 0; /*0x2b48c*/
            do /*0x2b530*/
            {
              if ( 0xAAAAAAAAAAAAAAABLL * ((v86 - v85) >> 3) <= v88 ) /*0x2b4a8*/
                v89 = v148; /*0x2b4a8*/
              else
                v89 = (_QWORD *)(v85 + v87); /*0x2b4a8*/
              *(_QWORD *)(v84 + v87) = v77 + *v89 - v82; /*0x2b4b8*/
              v85 = v146[0]; /*0x2b4c0*/
              v90 = (_QWORD *)(v146[0] + v87); /*0x2b4d0*/
              if ( 0xAAAAAAAAAAAAAAABLL * ((__int64)(v146[1] - v146[0]) >> 3) <= v88 ) /*0x2b4d8*/
                v91 = v148; /*0x2b4d8*/
              else
                v91 = (_QWORD *)(v146[0] + v87); /*0x2b4d8*/
              v92 = v91[1]; /*0x2b4dc*/
              v84 = s; /*0x2b4e0*/
              v93 = s + v87; /*0x2b4ec*/
              *(_QWORD *)(s + v87 + 8) = v77 + v92 - v82; /*0x2b4f0*/
              v86 = v146[1]; /*0x2b4f4*/
              v87 += 24; /*0x2b4f8*/
              if ( 0xAAAAAAAAAAAAAAABLL * ((v146[1] - v85) >> 3) <= v88 ) /*0x2b50c*/
                v90 = v148; /*0x2b50c*/
              ++v88; /*0x2b514*/
              *(_BYTE *)(v93 + 16) = *((_BYTE *)v90 + 16); /*0x2b518*/
            }
            while ( v88 < 0xAAAAAAAAAAAAAAABLL * ((__int64)(v131 - v84) >> 3) ); /*0x2b530*/
          }
          v133 = v77 + v83; /*0x2b53c*/
          v134 = v77 + v83; /*0x2b540*/
          v94 = v105; /*0x2b55c*/
          v137 = v77 + v151 - v82; /*0x2b564*/
          v139 = v77 + v153 - v82; /*0x2b56c*/
          v135 = 0; /*0x2b584*/
          v136 = v77 + v150 - v82; /*0x2b588*/
          v138 = v152; /*0x2b58c*/
          v140 = v77 + v154 - v82; /*0x2b590*/
          LOBYTE(v141) = v155; /*0x2b594*/
          v143 = v136; /*0x2b598*/
          v142 = v156; /*0x2b59c*/
          if ( v85 ) /*0x2b5a0*/
          {
            v146[1] = v85; /*0x2b5a4*/
            sub_3684BC(v85); /*0x2b5a8*/
          }
          v95 = *(_QWORD *)&v162[48]; /*0x2b5ac*/
          v3 = v104; /*0x2b5b0*/
          if ( *(_QWORD *)&v162[48] ) /*0x2b5b4*/
          {
            v96 = (unsigned __int64 *)(*(_QWORD *)&v162[48] + 8LL); /*0x2b5b8*/
            do /*0x2b5c4*/
              v97 = __ldaxr(v96); /*0x2b5bc*/
            while ( __stlxr(v97 - 1, v96) ); /*0x2b5c4*/
            if ( !v97 ) /*0x2b5cc*/
            {
              (*(void (__fastcall **)(__int64))(*(_QWORD *)v95 + 16LL))(v95); /*0x2b5dc*/
              sub_362874(v95); /*0x2b5e4*/
            }
          }
          sub_35DCBC(v162); /*0x2b5ec*/
          if ( s ) /*0x2b5f4*/
          {
            v131 = s; /*0x2b5f8*/
            sub_3684BC(s); /*0x2b5fc*/
            if ( (v105 & 1) != 0 ) /*0x2b600*/
              goto LABEL_9; /*0x2b600*/
            goto LABEL_188; /*0x2b600*/
          }
LABEL_187:
          if ( (v94 & 1) != 0 ) /*0x2b68c*/
            goto LABEL_9; /*0x2b68c*/
LABEL_188:
          v56 += 10; /*0x2b690*/
          if ( v56 == v109 ) /*0x2b69c*/
          {
            v4 = v102; /*0x2b6a0*/
            v6 = v103; /*0x2b6a4*/
            goto LABEL_190; /*0x2b6a4*/
          }
        }
        if ( v62 == -683417363 ) /*0x2b200*/
          break; /*0x2b200*/
        if ( v62 != -911318012 ) /*0x2b210*/
          goto LABEL_188; /*0x2b210*/
        v63 = *(unsigned __int8 *)(v5 + 48); /*0x2b214*/
        if ( (v63 & 1) != 0 ) /*0x2b228*/
          v64 = *(_QWORD *)(v5 + 64); /*0x2b228*/
        else
          LODWORD(v64) = v57; /*0x2b228*/
        if ( (v63 & 1) != 0 ) /*0x2b22c*/
          v65 = *(_QWORD *)(v5 + 56); /*0x2b22c*/
        else
          v65 = v63 >> 1; /*0x2b22c*/
        sub_1F560(v146, v64, v65); /*0x2b238*/
        v66 = sub_2BDCA8(v146, v56 + 4); /*0x2b248*/
        if ( HIBYTE(v147) >= 0x40u ) /*0x2b254*/
          sub_1F748(v146); /*0x2b25c*/
        if ( (v66 & 1) == 0 ) /*0x2b260*/
          goto LABEL_188; /*0x2b260*/
LABEL_9:
        v8 = (unsigned __int8)*v107; /*0x2a7cc*/
        if ( (v8 & 1) != 0 ) /*0x2a7d4*/
        {
          v10 = *(_QWORD *)(v5 + 56); /*0x2a7e4*/
          v9 = *(_QWORD *)(v5 + 64); /*0x2a7e4*/
        }
        else
        {
          v9 = v106; /*0x2a7d8*/
          v10 = v8 >> 1; /*0x2a7dc*/
        }
        netht_str_to_upper_hex(v9, v10, 1, v120); /*0x2a7f0*/
        if ( (__int64 *)((__int64 (__fastcall *)(__int64 *, _QWORD *))loc_372DC)(&qword_46C700, v120) != &qword_46C708 ) /*0x2a810*/
          goto LABEL_13; /*0x2a810*/
        sub_37448(&qword_46C700, v120, v120); /*0x2a83c*/
        if ( (sub_2301E0(*v56) & 1) != 0 ) /*0x2a848*/
          v11 = (*((unsigned __int8 *)v56 + 8) >> 1) & 1; /*0x2a850*/
        else
          v11 = 0; /*0x2a858*/
        v119 = 23; /*0x2a860*/
        v12 = v106; /*0x2a864*/
        v118[0] = 0; /*0x2a868*/
        if ( (*v107 & 1) != 0 ) /*0x2a870*/
          v12 = *(_QWORD *)(v5 + 64); /*0x2a874*/
        if ( (sub_1D7214(v12) & 1) == 0 ) /*0x2a87c*/
        {
          v108 = v11; /*0x2a9bc*/
          v19 = v3; /*0x2a9c0*/
          v22 = 0; /*0x2a9c4*/
          v23 = 0; /*0x2a9c8*/
          goto LABEL_29; /*0x2a9c8*/
        }
        memset(&s, 0, 0x80u); /*0x2a88c*/
        if ( (*v107 & 1) != 0 ) /*0x2a898*/
          v106 = *(_QWORD *)(v5 + 64); /*0x2a8a0*/
        if ( !(unsigned int)sub_14A710(v106, &s) ) /*0x2a8ac*/
        {
          memset(v162, 0, sizeof(v162)); /*0x2a8d0*/
          v161 = 0u; /*0x2a8d4*/
          v160 = 0u; /*0x2a8d8*/
          v159 = 0u; /*0x2a8dc*/
          v158 = 0u; /*0x2a8e0*/
          memset(v146, 0, 0x100u); /*0x2a8e4*/
          v144 = 25; /*0x2a8ec*/
          strcpy(v145, "<j5<j"); /*0x2a8fc*/
          for ( k = 0; k != 5; ++k ) /*0x2a918*/
            v145[k] ^= v144; /*0x2a928*/
          v145[5] = 0; /*0x2a938*/
          v14 = sub_1D938C((int)&v158, 64, &v139); /*0x2a94c*/
          v15 = sub_1D938C((int)v162, 64, &v141); /*0x2a95c*/
          v108 = v11; /*0x2a970*/
          sprintf((char *)v146, v145, v14, v15); /*0x2a974*/
          v16 = sub_1FC10(v146); /*0x2a97c*/
          v17 = sub_376D0(v118, v146, v16); /*0x2a98c*/
          v18 = v143; /*0x2a990*/
          v19 = v3; /*0x2a994*/
          v20 = sub_2650D8(v17); /*0x2a998*/
          v21 = v141; /*0x2a9a0*/
          v22 = (unsigned int)v20 - v18; /*0x2a9b0*/
          v23 = (unsigned int)sub_2650D8(v20) - v21; /*0x2a9b4*/
LABEL_29:
          v24 = *v56; /*0x2a9cc*/
          v144 = 31; /*0x2a9d8*/
          v25 = 0; /*0x2a9e8*/
          strcpy(v145, "}L@AH{HIC]EO"); /*0x2a9f0*/
          do /*0x2aae8*/
          {
            v145[v25] ^= (_BYTE)v25 + (_BYTE)v144; /*0x2aadc*/
            ++v25; /*0x2aae0*/
          }
          while ( v25 != 12 ); /*0x2aae8*/
          v145[12] = 0; /*0x2aaec*/
          sub_1E300(v117, v145, v116); /*0x2aaf8*/
          v26 = *(unsigned __int8 *)(v5 + 48); /*0x2aafc*/
          if ( (v26 & 1) != 0 ) /*0x2ab0c*/
            v27 = *(_QWORD *)(v5 + 64); /*0x2ab0c*/
          else
            LODWORD(v27) = v57; /*0x2ab0c*/
          if ( (v26 & 1) != 0 ) /*0x2ab10*/
            v28 = *(_QWORD *)(v5 + 56); /*0x2ab10*/
          else
            v28 = v26 >> 1; /*0x2ab10*/
          sub_1F560(v115, v27, v28); /*0x2ab1c*/
          sub_29014(v114, v118); /*0x2ab28*/
          sub_1E300(v113, &unk_39DD38, v112); /*0x2ab44*/
          sub_1E300(v111, &unk_39DD38, v110); /*0x2ab54*/
          LOBYTE(v101) = 1; /*0x2ab80*/
          sub_2B920(&v123, v24, v117, v115, v114, v113, v111, v108, v101, v22, v23); /*0x2ab84*/
          v29 = sub_23020C(*v56); /*0x2ab90*/
          v3 = v19; /*0x2ab94*/
          v30 = *(unsigned __int8 *)(sub_26173C() + 167); /*0x2ab9c*/
          v31 = v129; /*0x2aba0*/
          v129 &= v30; /*0x2aba8*/
          sub_2F3308((__int64)&v123); /*0x2abb0*/
          v32 = v29 ^ 1; /*0x2abb4*/
          v129 = v31; /*0x2abb8*/
          if ( !v31 && ((v29 ^ 1) & 1) != 0 ) /*0x2abc0*/
          {
            v33 = 0; /*0x2abc4*/
            if ( !v30 ) /*0x2abc8*/
              goto LABEL_40; /*0x2abc8*/
LABEL_45:
            if ( v32 & 1 | ((v33 & 1) == 0) ) /*0x2ac44*/
            {
              if ( !v129 ) /*0x2ac48*/
                goto LABEL_85; /*0x2ac48*/
LABEL_49:
              sub_230B68(1); /*0x2ac54*/
              v34 = netht_ctx_get_instance_576(); /*0x2ac5c*/
              v35 = sub_2653A8(v34, 1); /*0x2ac64*/
              if ( v123 != 25 ) /*0x2ac70*/
              {
                v36 = *(__int64 (__fastcall **)(_QWORD))(qword_4C2298 + 8); /*0x2ac80*/
                v37 = sub_26173C(); /*0x2ac84*/
                v35 = v36(*(unsigned int *)(v37 + 108)); /*0x2ac8c*/
              }
              v38 = sub_91EE0(v35); /*0x2ac90*/
              v39 = sub_9239C(v38, 307062571); /*0x2ac9c*/
              v40 = sub_12E744(v39); /*0x2aca0*/
              sub_364798(&v158, v40); /*0x2aca8*/
              v41 = sub_26F618(); /*0x2acac*/
              (*(void (__fastcall **)(_QWORD *__return_ptr))(*(_QWORD *)v41 + 8LL))(v146); /*0x2acc0*/
              strcpy(v162, "10jtgeqtf;78f5ygjh3ih"); /*0x2acc8*/
              for ( m = 0; m != 21; ++m ) /*0x2ad3c*/
                v162[m] -= 2; /*0x2ad68*/
              v131 = 0; /*0x2ad7c*/
              s = 0; /*0x2ad80*/
              v132 = 0; /*0x2ad84*/
              v43 = strlen(v162); /*0x2ad88*/
              v44 = v43; /*0x2ad8c*/
              if ( v43 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x2ad94*/
                sub_1EA30(&s); /*0x2b6c8*/
              if ( v43 >= 0x17 ) /*0x2ad9c*/
              {
                v46 = (v43 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x2adb8*/
                v45 = (char *)sub_368454(v46); /*0x2adc4*/
                v132 = v45; /*0x2adcc*/
                s = v46 | 1; /*0x2add0*/
                v131 = v44; /*0x2add4*/
              }
              else
              {
                v45 = (char *)&s + 1; /*0x2ada0*/
                LOBYTE(s) = 2 * v43; /*0x2ada8*/
                if ( !v43 ) /*0x2adac*/
                {
LABEL_59:
                  v45[v44] = 0; /*0x2ade8*/
                  sub_23640(v162, v146, &s); /*0x2adf8*/
                  if ( (v158 & 1) != 0 ) /*0x2ae18*/
                    v47 = *((_QWORD *)&v158 + 1); /*0x2ae18*/
                  else
                    v47 = (unsigned __int64)(unsigned __int8)v158 >> 1; /*0x2ae18*/
                  v48 = (*(__int64 (__fastcall **)(unsigned __int64))(qword_4C2298 + 352))(v47 + 1); /*0x2ae24*/
                  v49 = v48; /*0x2ae28*/
                  if ( v48 ) /*0x2ae2c*/
                  {
                    if ( (v158 & 1) != 0 ) /*0x2ae54*/
                      v50 = *((_QWORD *)&v158 + 1); /*0x2ae54*/
                    else
                      v50 = (unsigned __int64)(unsigned __int8)v158 >> 1; /*0x2ae54*/
                    if ( (v158 & 1) != 0 ) /*0x2ae58*/
                      v51 = (char *)v159; /*0x2ae58*/
                    else
                      v51 = (char *)&v158 + 1; /*0x2ae58*/
                    (*(void (__fastcall **)(__int64, char *, unsigned __int64))(qword_4C2298 + 392))(v48, v51, v50); /*0x2ae60*/
                    if ( (v162[0] & 1) != 0 ) /*0x2ae80*/
                      v52 = *(char **)&v162[16]; /*0x2ae80*/
                    else
                      v52 = &v162[1]; /*0x2ae80*/
                    if ( (v158 & 1) != 0 ) /*0x2ae88*/
                      v53 = *((_QWORD *)&v158 + 1); /*0x2ae88*/
                    else
                      v53 = (unsigned __int64)(unsigned __int8)v158 >> 1; /*0x2ae88*/
                    sub_2E676C(v52, v49, v53); /*0x2ae90*/
                    v48 = (*(__int64 (__fastcall **)(__int64))(qword_4C2298 + 360))(v49); /*0x2aea8*/
                  }
                  if ( (v162[0] & 1) != 0 ) /*0x2aeb0*/
                    v48 = sub_3684BC(*(_QWORD *)&v162[16]); /*0x2aeb8*/
                  if ( (s & 1) != 0 ) /*0x2aec0*/
                    v48 = sub_3684BC(v132); /*0x2aec8*/
                  if ( (v146[0] & 1) != 0 ) /*0x2aed0*/
                    v48 = sub_3684BC(v147); /*0x2aed8*/
                  v54 = qword_4C26F8; /*0x2aee4*/
                  v55 = sub_343F98(v48); /*0x2aee8*/
                  sub_23A04(v54, v55, &loc_23748); /*0x2aefc*/
                  (*(void (__fastcall **)(_QWORD))(qword_4C2298 + 600))(0); /*0x2af14*/
                  if ( (v158 & 1) != 0 ) /*0x2af1c*/
                    sub_3684BC(v159); /*0x2af24*/
                  goto LABEL_85; /*0x2af28*/
                }
              }
              memcpy(v45, v162, v44); /*0x2ade4*/
              goto LABEL_59; /*0x2ade4*/
            }
            if ( v129 ) /*0x2ac50*/
              goto LABEL_49; /*0x2ac50*/
LABEL_84:
            sub_230B68(7); /*0x2af2c*/
            goto LABEL_85; /*0x2af30*/
          }
          HIBYTE(v148[0]) = 23; /*0x2abe4*/
          LOBYTE(v146[1]) = 0; /*0x2abe8*/
          LODWORD(v146[0]) = v123; /*0x2abec*/
          sub_1EB70(&v146[1], v124); /*0x2abf4*/
          HIDWORD(v146[0]) = (*(__int64 (__fastcall **)(_QWORD))(qword_4C2298 + 400))(0); /*0x2ac10*/
          netht_ctx_get_instance_576(); /*0x2ac14*/
          v33 = sub_266F14(v146); /*0x2ac20*/
          if ( HIBYTE(v148[0]) >= 0x40u ) /*0x2ac2c*/
            sub_1F748(&v146[1]); /*0x2ac34*/
          if ( v30 ) /*0x2ac38*/
            goto LABEL_45; /*0x2ac38*/
LABEL_40:
          if ( (v33 & 1) != 0 ) /*0x2abcc*/
            goto LABEL_84; /*0x2abcc*/
LABEL_85:
          if ( v128[23] >= 0x40u ) /*0x2af3c*/
            sub_1F748(v128); /*0x2af44*/
          if ( v127[23] >= 0x40u ) /*0x2af50*/
            sub_1F748(v127); /*0x2af58*/
          if ( v126[23] >= 0x40u ) /*0x2af64*/
            sub_1F748(v126); /*0x2af6c*/
          if ( v125[23] >= 0x40u ) /*0x2af78*/
            sub_1F748(v125); /*0x2af80*/
          if ( v124[23] >= 0x40u ) /*0x2af8c*/
            sub_1F748(v124); /*0x2af94*/
          if ( v111[23] >= 0x40u ) /*0x2afa0*/
            sub_1F748(v111); /*0x2afa8*/
          if ( v113[23] >= 0x40u ) /*0x2afb4*/
            sub_1F748(v113); /*0x2afbc*/
          if ( v114[23] >= 0x40u ) /*0x2afc8*/
            sub_1F748(v114); /*0x2afd0*/
          if ( v115[23] >= 0x40u ) /*0x2afdc*/
            sub_1F748(v115); /*0x2afe4*/
          if ( v117[23] >= 0x40u ) /*0x2aff0*/
            sub_1F748(v117); /*0x2aff8*/
        }
        if ( v119 >= 0x40u ) /*0x2b004*/
          sub_1F748(v118); /*0x2b00c*/
LABEL_13:
        if ( (v120[0] & 1) != 0 ) /*0x2a818*/
          sub_3684BC(v120[2]); /*0x2a820*/
        v4 = v102; /*0x2a824*/
        v6 = 1; /*0x2a828*/
LABEL_190:
        v5 += 72; /*0x2b6a8*/
        if ( v5 == v4 ) /*0x2b6b0*/
        {
LABEL_8:
          sub_2BA8C(&v121); /*0x2a7bc*/
          return v6 & 1; /*0x2a7c8*/
        }
      }
      v67 = *(unsigned __int8 *)(v5 + 48); /*0x2b268*/
      if ( (v67 & 1) != 0 ) /*0x2b27c*/
        v68 = *(_QWORD *)(v5 + 64); /*0x2b27c*/
      else
        LODWORD(v68) = v57; /*0x2b27c*/
      if ( (v67 & 1) != 0 ) /*0x2b280*/
        v69 = *(_QWORD *)(v5 + 56); /*0x2b280*/
      else
        v69 = v67 >> 1; /*0x2b280*/
      sub_1F560(v146, v68, v69); /*0x2b28c*/
      if ( (sub_2BDCA8(v146, v56 + 4) & 1) == 0 ) /*0x2b29c*/
      {
        v94 = 0; /*0x2b608*/
LABEL_185:
        if ( HIBYTE(v147) >= 0x40u ) /*0x2b680*/
          sub_1F748(v146); /*0x2b688*/
        goto LABEL_187; /*0x2b688*/
      }
      *(_DWORD *)v162 = 3; /*0x2b2a4*/
      strcpy(&v162[4], ",ejofp,"); /*0x2b2b0*/
      for ( n = 0; n != 7; ++n ) /*0x2b2e4*/
        v162[n + 4] ^= v162[0]; /*0x2b2f4*/
      v162[11] = 0; /*0x2b304*/
      v132 = 0; /*0x2b308*/
      v131 = 0; /*0x2b30c*/
      s = 0; /*0x2b310*/
      v71 = strlen(&v162[4]); /*0x2b314*/
      v72 = v71; /*0x2b318*/
      if ( v71 >= 0xFFFFFFFFFFFFFFF0LL ) /*0x2b320*/
        sub_1EA30(&s); /*0x2b6c0*/
      if ( v71 >= 0x17 ) /*0x2b328*/
      {
        v98 = v5; /*0x2b614*/
        v99 = v3; /*0x2b618*/
        v100 = (v71 + 16) & 0xFFFFFFFFFFFFFFF0LL; /*0x2b61c*/
        v73 = (char *)sub_368454(v100); /*0x2b628*/
        v132 = v73; /*0x2b630*/
        s = v100 | 1; /*0x2b634*/
        v131 = v72; /*0x2b638*/
        v3 = v99; /*0x2b63c*/
        v5 = v98; /*0x2b640*/
      }
      else
      {
        v73 = (char *)&s + 1; /*0x2b32c*/
        LOBYTE(s) = 2 * v71; /*0x2b334*/
        if ( !v71 ) /*0x2b338*/
        {
LABEL_183:
          v73[v72] = 0; /*0x2b654*/
          v94 = sub_2BDBFC(v107, &s); /*0x2b664*/
          if ( (s & 1) != 0 ) /*0x2b66c*/
            sub_3684BC(v132); /*0x2b674*/
          goto LABEL_185; /*0x2b674*/
        }
      }
      memcpy(v73, &v162[4], v72); /*0x2b650*/
      goto LABEL_183; /*0x2b650*/
    }
  }
  LOBYTE(v6) = 0; /*0x2a780*/
  return v6 & 1; /*0x2a7b4*/
}