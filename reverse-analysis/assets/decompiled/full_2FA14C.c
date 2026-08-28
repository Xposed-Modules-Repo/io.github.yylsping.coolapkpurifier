__int64 __fastcall sub_2FA14C(__int64 a1, __int64 a2, unsigned int a3)
{
  __int64 v6; // x20
  __int64 v7; // x8
  size_t v8; // x0
  size_t v9; // x23
  char *v10; // x24
  unsigned __int64 v11; // x25
  __int64 v12; // x0
  int v13; // w20
  __int64 v14; // x0
  __int64 v15; // x0
  __int64 v16; // x0
  __int128 v17; // q0
  __int64 v18; // x19
  __int64 v19; // x20
  __int64 v20; // x0
  int v21; // w19
  __int64 v22; // x0
  __int128 v23; // q0
  __int64 v24; // x0
  __int64 v25; // x0
  __int64 v26; // x8
  __int64 i; // x8
  unsigned __int64 v28; // x0
  size_t v29; // x24
  char *v30; // x25
  unsigned __int64 v31; // x26
  void *v32; // x8
  __int64 v33; // x28
  int v34; // w27
  char *v35; // x19
  unsigned __int64 v36; // x9
  __int64 v37; // x0
  char *v38; // x1
  unsigned __int64 v39; // x2
  char *v40; // x23
  char *v41; // x8
  __int64 v42; // x22
  char *v43; // x24
  char v44; // w8
  __int64 v45; // x8
  __int128 v46; // q0
  char *v47; // x22
  char v48; // t1
  int v49; // w8
  __int64 v50; // x8
  size_t v51; // x0
  __int64 j; // x8
  size_t v53; // x0
  __int64 v54; // x8
  size_t v55; // x0
  __int64 k; // x8
  size_t v57; // x0
  _BOOL4 v58; // w22
  __int64 v59; // x8
  size_t v60; // x0
  __int64 m; // x8
  size_t v62; // x0
  __int64 n; // x8
  size_t v64; // x0
  __int64 ii; // x8
  size_t v66; // x2
  char *v67; // x1
  size_t v68; // x2
  char *v69; // x1
  unsigned __int64 v70; // x10
  size_t v71; // x2
  unsigned __int64 v72; // x11
  char *v73; // x0
  unsigned __int8 *v74; // x1
  __int64 v75; // x8
  unsigned __int8 *v76; // x9
  __int64 mm; // x8
  size_t v78; // x2
  char *v79; // x1
  unsigned __int64 v80; // x8
  __int64 v81; // x1
  size_t v82; // x2
  size_t v83; // x2
  char *v84; // x1
  size_t v85; // x2
  char *v86; // x1
  size_t v87; // x2
  char *v88; // x1
  __int64 v89; // x0
  unsigned __int64 v90; // x8
  __int64 jj; // x8
  size_t v92; // x2
  char *v93; // x1
  int v94; // w19
  __int64 kk; // x8
  size_t v96; // x0
  __int64 v97; // x0
  size_t v98; // x2
  char *v99; // x1
  size_t v100; // x0
  size_t v101; // x2
  char *v102; // x1
  size_t v103; // x0
  size_t v104; // x2
  char *v105; // x1
  __int64 v106; // x0
  int v107; // w0
  int v108; // w0
  __int64 v109; // x0
  __int64 v110; // x0
  __int64 v111; // x0
  __int64 v112; // x26
  __int64 v113; // x0
  char *v114; // x23
  __int64 v115; // x0
  char *v116; // x8
  __int64 v117; // x24
  char *v118; // x21
  char *v119; // x25
  char v120; // w8
  __int64 v121; // x8
  __int128 v122; // q0
  __int64 nn; // x8
  size_t v124; // x0
  size_t v125; // x24
  char *v126; // x25
  char *v127; // x24
  char v128; // t1
  int *v129; // x8
  __int64 v130; // x10
  int v131; // w9
  __int64 v132; // x22
  __int64 v133; // x8
  int v134; // w10
  __int64 v135; // x8
  __int64 i1; // x8
  __int64 v137; // x8
  size_t v138; // x0
  size_t v139; // x23
  char *v140; // x24
  unsigned __int64 v141; // x25
  __int64 v142; // x8
  __int64 **v143; // x0
  _QWORD *v144; // x1
  __int64 v145; // x0
  int v146; // w22
  int v147; // w19
  __int64 v148; // x0
  __int128 v149; // q0
  __int64 v150; // x0
  __int64 v151; // x0
  _DWORD *v152; // x0
  __int64 v153; // x8
  __int64 v154; // x0
  __int64 v155; // x8
  __int64 result; // x0
  _QWORD *v157; // x21
  char *v158; // x20
  unsigned __int64 v159; // x21
  size_t v160; // x0
  unsigned __int8 *v161; // x9
  char *v162; // x8
  size_t v163; // x11
  unsigned __int8 *v164; // x12
  int v165; // t1
  __int64 v166; // x9
  void (__fastcall *v167)(char *, __int64, char *, _QWORD *, char *); // x8
  _QWORD *v168; // x3
  char *v169; // x4
  size_t v170; // x0
  size_t v171; // x20
  char *v172; // x21
  unsigned __int64 v173; // x22
  __int64 v174; // x19
  __int64 i2; // x24
  _DWORD *v176; // x27
  _DWORD *v177; // x28
  char *v178; // x1
  size_t v179; // x2
  __int64 v180; // x0
  size_t v181; // x0
  __int64 v182; // x0
  size_t v183; // x2
  char *v184; // x1
  __int128 *v185; // x1
  __int64 v186; // x2
  __int64 v187; // x8
  char v188; // w8
  unsigned __int64 v189; // x9
  __int64 v190; // x10
  void (__fastcall *v191)(char *, __int64, char *, _QWORD *, _OWORD *); // x9
  _QWORD *v192; // x3
  _OWORD *v193; // x4
  __int64 i3; // x8
  unsigned __int64 v195; // x8
  int v196; // w11
  unsigned __int64 v197; // x10
  int v198; // w8
  __int64 v199; // x0
  _QWORD *v200; // x0
  int *v202; // x8
  __int64 v203; // x10
  int v204; // w9
  __int64 v205; // x22
  __int64 v206; // x8
  int v207; // w10
  __int64 v208; // x8
  unsigned __int64 v209; // x12
  __int64 v210; // x9
  _DWORD *v211; // x8
  __int64 v212; // x8
  __int64 **v213; // x0
  _QWORD *v214; // x1
  __int64 v215; // x8
  __int64 v216; // x8
  __int64 **v217; // x0
  _QWORD *v218; // x1
  _DWORD *v219; // x19
  _DWORD *v220; // x20
  char *v221; // x1
  size_t v222; // x2
  __int64 v223; // x0
  size_t v224; // x0
  __int64 v225; // x0
  char *v226; // x1
  size_t v227; // x2
  __int64 *v228; // x1
  __int64 v229; // x2
  int v230; // [xsp+0h] [xbp-980h]
  __int64 v231; // [xsp+70h] [xbp-910h]
  void *v232; // [xsp+A8h] [xbp-8D8h]
  __int64 v233; // [xsp+B0h] [xbp-8D0h]
  unsigned int v234; // [xsp+BCh] [xbp-8C4h]
  __int64 v235; // [xsp+C0h] [xbp-8C0h]
  unsigned int v236; // [xsp+C8h] [xbp-8B8h]
  _BYTE v237[24]; // [xsp+D0h] [xbp-8B0h] BYREF
  _BYTE v238[24]; // [xsp+E8h] [xbp-898h] BYREF
  _BYTE v239[24]; // [xsp+100h] [xbp-880h] BYREF
  _BYTE v240[24]; // [xsp+118h] [xbp-868h] BYREF
  _BYTE v241[28]; // [xsp+130h] [xbp-850h] BYREF
  int v242; // [xsp+14Ch] [xbp-834h] BYREF
  __int128 v243; // [xsp+150h] [xbp-830h] BYREF
  char *v244; // [xsp+160h] [xbp-820h]
  __int128 v245; // [xsp+170h] [xbp-810h] BYREF
  __int64 v246; // [xsp+180h] [xbp-800h]
  __int128 v247; // [xsp+190h] [xbp-7F0h] BYREF
  _OWORD *v248; // [xsp+1A0h] [xbp-7E0h]
  _BYTE v249[23]; // [xsp+1B0h] [xbp-7D0h] BYREF
  unsigned __int8 v250; // [xsp+1C7h] [xbp-7B9h]
  _QWORD v251[2]; // [xsp+1C8h] [xbp-7B8h] BYREF
  unsigned __int8 v252; // [xsp+1DFh] [xbp-7A1h]
  _BYTE v253[24]; // [xsp+1E0h] [xbp-7A0h] BYREF
  _BYTE v254[24]; // [xsp+1F8h] [xbp-788h] BYREF
  _BYTE v255[24]; // [xsp+210h] [xbp-770h] BYREF
  _BYTE v256[24]; // [xsp+228h] [xbp-758h] BYREF
  _BYTE v257[24]; // [xsp+240h] [xbp-740h] BYREF
  _BYTE v258[24]; // [xsp+258h] [xbp-728h] BYREF
  _BYTE v259[24]; // [xsp+270h] [xbp-710h] BYREF
  _BYTE v260[24]; // [xsp+288h] [xbp-6F8h] BYREF
  __int64 v261; // [xsp+2A0h] [xbp-6E0h] BYREF
  size_t v262; // [xsp+2A8h] [xbp-6D8h]
  char *v263; // [xsp+2B0h] [xbp-6D0h]
  _QWORD v264[2]; // [xsp+2B8h] [xbp-6C8h] BYREF
  char *v265; // [xsp+2C8h] [xbp-6B8h]
  __int128 v266; // [xsp+2D0h] [xbp-6B0h] BYREF
  unsigned __int64 v267; // [xsp+2E0h] [xbp-6A0h]
  __int128 v268; // [xsp+2F0h] [xbp-690h] BYREF
  unsigned __int64 v269; // [xsp+300h] [xbp-680h]
  __int64 v270; // [xsp+308h] [xbp-678h] BYREF
  size_t v271; // [xsp+310h] [xbp-670h]
  __int64 v272; // [xsp+318h] [xbp-668h]
  int v273; // [xsp+320h] [xbp-660h]
  char v274[12]; // [xsp+324h] [xbp-65Ch] BYREF
  size_t v275; // [xsp+330h] [xbp-650h]
  char *v276; // [xsp+338h] [xbp-648h]
  int v277; // [xsp+340h] [xbp-640h]
  char v278[20]; // [xsp+344h] [xbp-63Ch] BYREF
  __int64 v279; // [xsp+358h] [xbp-628h]
  __int128 v280; // [xsp+360h] [xbp-620h] BYREF
  __int64 v281; // [xsp+370h] [xbp-610h]
  __int64 v282; // [xsp+380h] [xbp-600h] BYREF
  __int64 v283; // [xsp+388h] [xbp-5F8h]
  unsigned __int8 v284; // [xsp+397h] [xbp-5E9h]
  _QWORD v285[2]; // [xsp+398h] [xbp-5E8h] BYREF
  char *v286; // [xsp+3A8h] [xbp-5D8h]
  __int128 v287; // [xsp+3B0h] [xbp-5D0h] BYREF
  char *v288; // [xsp+3C0h] [xbp-5C0h]
  char v289[8]; // [xsp+3D8h] [xbp-5A8h] BYREF
  size_t v290; // [xsp+3E0h] [xbp-5A0h]
  char *v291; // [xsp+3E8h] [xbp-598h]
  int v292; // [xsp+3F0h] [xbp-590h]
  char src[8]; // [xsp+458h] [xbp-528h] BYREF
  _DWORD *v294; // [xsp+460h] [xbp-520h]
  unsigned __int64 v295; // [xsp+468h] [xbp-518h]
  char v296[16]; // [xsp+470h] [xbp-510h] BYREF
  char *v297; // [xsp+480h] [xbp-500h]
  unsigned __int8 v298; // [xsp+48Fh] [xbp-4F1h]
  _BYTE v299[24]; // [xsp+490h] [xbp-4F0h] BYREF
  _BYTE v300[24]; // [xsp+4A8h] [xbp-4D8h] BYREF
  _BYTE v301[24]; // [xsp+4C0h] [xbp-4C0h] BYREF
  _BYTE v302[56]; // [xsp+4D8h] [xbp-4A8h] BYREF
  char v303[16]; // [xsp+510h] [xbp-470h] BYREF
  unsigned __int64 v304; // [xsp+520h] [xbp-460h]
  char v305; // [xsp+528h] [xbp-458h]
  char v306; // [xsp+529h] [xbp-457h]
  char v307; // [xsp+52Ah] [xbp-456h]
  char v308; // [xsp+52Bh] [xbp-455h]
  char v309; // [xsp+52Ch] [xbp-454h]
  char v310; // [xsp+52Dh] [xbp-453h]
  char v311; // [xsp+52Eh] [xbp-452h]
  unsigned __int8 v312; // [xsp+52Fh] [xbp-451h]
  _BYTE v313[4]; // [xsp+530h] [xbp-450h] BYREF
  char v314; // [xsp+534h] [xbp-44Ch]
  unsigned __int8 v315; // [xsp+547h] [xbp-439h]
  _BYTE v316[23]; // [xsp+548h] [xbp-438h] BYREF
  unsigned __int8 v317; // [xsp+55Fh] [xbp-421h]
  _BYTE v318[23]; // [xsp+560h] [xbp-420h] BYREF
  unsigned __int8 v319; // [xsp+577h] [xbp-409h]
  _BYTE v320[23]; // [xsp+578h] [xbp-408h] BYREF
  unsigned __int8 v321; // [xsp+58Fh] [xbp-3F1h]
  __int128 v322; // [xsp+910h] [xbp-70h] BYREF
  unsigned __int64 v323; // [xsp+920h] [xbp-60h]
  __int64 v324; // [xsp+928h] [xbp-58h]

  v324 = *(_QWORD *)(_ReadStatusReg(TPIDR_EL0) + 40);
  v271 = 0;
  v270 = 0;
  v272 = 0;
  v268 = 0u;
  v269 = 0;
  v6 = sub_264D24(a1);
  v7 = 0;
  strcpy(v296, "p}");
  do
    v296[v7++] -= 9;
  while ( v7 != 2 );
  memset(v303, 0, sizeof(v303));
  v304 = 0;
  v8 = strlen(v296);
  v9 = v8;
  if ( v8 >= 0xFFFFFFFFFFFFFFF0LL )
    sub_1EA30(v303);
  if ( v8 >= 0x17 )
  {
    v11 = (v8 + 16) & 0xFFFFFFFFFFFFFFF0LL;
    v10 = (char *)sub_368454(v11);
    v304 = (unsigned __int64)v10;
    *(_QWORD *)v303 = v11 | 1;
    *(_QWORD *)&v303[8] = v9;
    goto LABEL_8;
  }
  v10 = &v303[1];
  v303[0] = 2 * v8;
  if ( v8 )
LABEL_8:
    memcpy(v10, v296, v9);
  v10[v9] = 0;
  v12 = sub_266C30(v6, v303);
  v13 = v12;
  if ( (v303[0] & 1) != 0 )
    v12 = sub_3684BC(v304);
  if ( a3 == 6 && (v14 = sub_26173C(v12), v13 == 1) && *(_BYTE *)(v14 + 788) )
  {
    v15 = ((__int64 (*)(void))sub_276DDC)();
    sub_27BD94(v303, v15);
  }
  else
  {
    v16 = ((__int64 (*)(void))sub_276DDC)();
    sub_276E54(v303, v16);
  }
  sub_413EC(&v268);
  v17 = *(_OWORD *)v303;
  memset(v303, 0, sizeof(v303));
  v268 = v17;
  v269 = v304;
  v304 = 0;
  sub_409AC(v303);
  v18 = *((_QWORD *)&v268 + 1);
  v19 = v268;
  v20 = sub_266D20(a3, 8);
  v21 = 954437177 * ((unsigned __int64)(v18 - v19) >> 5);
  *(_DWORD *)(a2 + 532) = v21;
  v266 = 0u;
  v267 = 0;
  if ( a3 != 8 )
  {
    v22 = sub_26F618(v20);
    (*(void (__fastcall **)(char *__return_ptr))(*(_QWORD *)v22 + 200LL))(v303);
    sub_2A1B54(&v266);
    v23 = *(_OWORD *)v303;
    memset(v303, 0, sizeof(v303));
    v266 = v23;
    v267 = v304;
    v304 = 0;
    sub_301BC(v303);
  }
  v24 = sub_266D20(a3, 9);
  if ( v21 >= 1 )
  {
    v25 = sub_26F618(v24);
    (*(void (__fastcall **)(_QWORD *__return_ptr))(*(_QWORD *)v25 + 88LL))(v264);
    *(_DWORD *)v303 = 93;
    v26 = 0;
    v303[4] = 46;
    v303[5] = 59;
    v303[6] = 51;
    v303[7] = 6;
    v303[8] = 0;
    do
    {
      v303[v26 + 4] ^= (_BYTE)v26 + v303[0];
      ++v26;
    }
    while ( v26 != 4 );
    v303[8] = 0;
    v236 = sub_2E7F84(&v303[4]);
    *(_DWORD *)v303 = 58;
    v303[4] = 30;
    v303[5] = 105;
    v303[6] = 101;
    v303[7] = 120;
    v303[8] = 124;
    v303[9] = 25;
    strcpy(&v303[10], "{");
    for ( i = 0; i != 7; ++i )
      v303[i + 4] ^= v303[0];
    v303[11] = 0;
    v263 = 0;
    v262 = 0;
    v261 = 0;
    v28 = strlen(&v303[4]);
    v29 = v28;
    if ( v28 >= 0xFFFFFFFFFFFFFFF0LL )
      sub_1EA30(&v261);
    v233 = a1;
    if ( v28 >= 0x17 )
    {
      v31 = (v28 + 16) & 0xFFFFFFFFFFFFFFF0LL;
      v30 = (char *)sub_368454(v31);
      v263 = v30;
      v261 = v31 | 1;
      v262 = v29;
    }
    else
    {
      v30 = (char *)&v261 + 1;
      LOBYTE(v261) = 2 * v28;
      if ( !v28 )
        goto LABEL_29;
    }
    v28 = (unsigned __int64)memcpy(v30, &v303[4], v29);
LABEL_29:
    v32 = (void *)*((_QWORD *)&v268 + 1);
    v33 = v268;
    v30[v29] = 0;
    v235 = a2;
    v234 = a3;
    v232 = v32;
    if ( (void *)v33 != v32 )
    {
      v231 = a2 + 40;
      while ( 1 )
      {
        v34 = *(unsigned __int8 *)(v33 + 281);
        v28 = sub_1E984(v296, v33 + 72);
        v35 = (char *)v266;
        v290 = 0;
        *(_QWORD *)v289 = 0;
        v291 = 0;
        if ( *((_QWORD *)&v266 + 1) != (_QWORD)v266 )
        {
          do
          {
            v36 = *(unsigned __int8 *)(v33 + 48);
            v37 = v33 + 49;
            if ( (v36 & 1) != 0 )
              v37 = *(_QWORD *)(v33 + 64);
            if ( (*v35 & 1) != 0 )
            {
              v38 = (char *)*((_QWORD *)v35 + 2);
              if ( (v36 & 1) != 0 )
              {
LABEL_36:
                v39 = *(_QWORD *)(v33 + 56);
                goto LABEL_39;
              }
            }
            else
            {
              v38 = v35 + 1;
              if ( (v36 & 1) != 0 )
                goto LABEL_36;
            }
            v39 = v36 >> 1;
LABEL_39:
            v28 = (*(__int64 (__fastcall **)(__int64, char *, unsigned __int64))(qword_4C2298 + 328))(v37, v38, v39);
            if ( !(_DWORD)v28 )
            {
              v41 = v35 + 24;
              if ( v35 + 24 == *((char **)&v266 + 1) )
              {
                v40 = v35;
              }
              else
              {
                v42 = *((_QWORD *)&v266 + 1) - 24LL;
                v40 = v35;
                do
                {
                  v43 = v40;
                  if ( (*v40 & 1) != 0 )
                  {
                    **((_BYTE **)v40 + 2) = 0;
                    v44 = *v40;
                    *((_QWORD *)v40 + 1) = 0;
                    if ( (v44 & 1) != 0 )
                    {
                      v28 = sub_3684BC(*((_QWORD *)v40 + 2));
                      *(_QWORD *)v40 = 0;
                    }
                  }
                  else
                  {
                    *(_WORD *)v40 = 0;
                  }
                  v45 = *((_QWORD *)v40 + 5);
                  v46 = *(_OWORD *)(v40 + 24);
                  v40 += 24;
                  *((_QWORD *)v43 + 3) = 0;
                  *((_QWORD *)v43 + 4) = 0;
                  *((_QWORD *)v43 + 2) = v45;
                  *(_OWORD *)v43 = v46;
                  *((_QWORD *)v43 + 5) = 0;
                }
                while ( (char *)v42 != v43 + 24 );
                v41 = (char *)*((_QWORD *)&v266 + 1);
                if ( *((char **)&v266 + 1) == v40 )
                  goto LABEL_55;
              }
              v47 = v41;
              do
              {
                v48 = *(v47 - 24);
                v47 -= 24;
                if ( (v48 & 1) != 0 )
                  v28 = sub_3684BC(*((_QWORD *)v41 - 1));
                v41 = v47;
              }
              while ( v40 != v47 );
LABEL_55:
              *((_QWORD *)&v266 + 1) = v40;
              continue;
            }
            v40 = (char *)*((_QWORD *)&v266 + 1);
            v35 += 24;
          }
          while ( v40 != v35 );
        }
        if ( !v34 )
          break;
LABEL_58:
        *(_BYTE *)(v33 + 280) = 1;
        if ( (v289[0] & 1) != 0 )
          v28 = sub_3684BC(v291);
        a2 = v235;
        a3 = v234;
        if ( (v296[0] & 1) != 0 )
          v28 = sub_3684BC(v297);
        v33 += 288;
        if ( (void *)v33 == v232 )
          goto LABEL_263;
      }
      if ( (sub_2300E0(22) & 1) != 0 )
      {
        v49 = *(unsigned __int8 *)(v33 + 96);
        if ( *(_BYTE *)(v33 + 96) )
        {
          v49 = *(unsigned __int8 *)(v33 + 97);
          if ( *(_BYTE *)(v33 + 97) )
          {
            v49 = *(unsigned __int8 *)(v33 + 102);
            if ( *(_BYTE *)(v33 + 102) )
            {
              *(_DWORD *)v303 = 121;
              v303[4] = 57;
              v303[5] = 58;
              v50 = 0;
              v303[6] = 8;
              v303[7] = 9;
              v303[8] = 93;
              v303[9] = 31;
              v303[10] = 17;
              v303[11] = -28;
              v303[12] = -95;
              v303[13] = -29;
              v303[14] = -17;
              v303[15] = -31;
              v304 = 0xFEABEEE7E9A7F2F7LL;
              v305 = -24;
              v306 = -17;
              v307 = -21;
              v308 = -13;
              v309 = -3;
              v310 = -5;
              v311 = -29;
              v312 = 0;
              do
              {
                v303[v50 + 4] ^= (_BYTE)v50 + v303[0];
                ++v50;
              }
              while ( v50 != 27 );
              v312 = 0;
              v51 = strlen(&v303[4]);
              sub_1FDD0((int)v289, &v303[4], v51);
              sub_3009BC(v233, v33 + 48, v296);
              v49 = 1;
            }
          }
        }
        if ( *(_BYTE *)(v33 + 98) )
        {
          strcpy(v303, "CClqmhfw");
          for ( j = 0; j != 8; ++j )
            v303[j] -= 3;
          v53 = strlen(v303);
          sub_2331C((int)v289, v303, v53);
          v49 = 1;
        }
        if ( *(_BYTE *)(v33 + 97) )
        {
          if ( !*(_BYTE *)(v33 + 99) )
            goto LABEL_559;
          memset(v303, 75, 2);
          v303[2] = -126;
          qmemcpy(&v303[3], "}t", 2);
          v303[5] = 127;
          v54 = 0;
          strcpy(&v303[6], "p~pn");
          do
            v303[v54++] -= 11;
          while ( v54 != 10 );
          v55 = strlen(v303);
          sub_2331C((int)v289, v303, v55);
          v49 = 1;
          if ( *(_BYTE *)(v33 + 97) )
          {
LABEL_559:
            if ( *(_BYTE *)(v33 + 100) )
            {
              strcpy(v303, "AAcjoebdd");
              for ( k = 0; k != 9; ++k )
                --v303[k];
              v57 = strlen(v303);
              sub_2331C((int)v289, v303, v57);
              v49 = 1;
            }
          }
        }
        if ( *(_BYTE *)(v33 + 101) )
        {
          *(_DWORD *)v303 = 106;
          v59 = 0;
          v303[4] = 42;
          v303[5] = 43;
          v303[6] = 25;
          v303[7] = 3;
          v303[8] = 5;
          v303[9] = 1;
          v303[10] = 31;
          v303[11] = 6;
          v303[12] = 28;
          v303[13] = 0;
          do
          {
            v303[v59 + 4] ^= (_BYTE)v59 + v303[0];
            ++v59;
          }
          while ( v59 != 9 );
          v303[13] = 0;
          v60 = strlen(&v303[4]);
          sub_2331C((int)v289, &v303[4], v60);
          v49 = 1;
        }
        if ( *(_BYTE *)(v33 + 103) )
        {
          strcpy(v303, "BBujk|wmw");
          for ( m = 0; m != 9; ++m )
            v303[m] -= 2;
          v62 = strlen(v303);
          sub_2331C((int)v289, v303, v62);
          v49 = 1;
        }
        v58 = v49 != 0;
        if ( *(_BYTE *)(v33 + 104) )
        {
          *(_DWORD *)v303 = 65;
          v303[4] = 1;
          v303[5] = 1;
          strcpy(&v303[6], ",.\"*-.\"");
          for ( n = 4; n != 13; ++n )
            v303[n] ^= v303[0];
          v303[13] = 0;
          v64 = strlen(&v303[4]);
          sub_2331C((int)v289, &v303[4], v64);
          v58 = 1;
        }
      }
      else
      {
        v58 = 0;
      }
      if ( (sub_2BDADC(v264, v33 + 48) & 1) != 0 && v236 != *(_DWORD *)(v33 + 196) )
      {
        *(_DWORD *)src = 71;
        strcpy(&src[4], "2.#");
        for ( ii = 0; ii != 3; ++ii )
          src[ii + 4] ^= src[0];
        src[7] = 0;
        sub_1E300(v260, &src[4], v251);
        sub_364088(&v287, v236);
        if ( (v287 & 1) != 0 )
          v66 = *((_QWORD *)&v287 + 1);
        else
          v66 = (unsigned __int64)(unsigned __int8)v287 >> 1;
        if ( (v287 & 1) != 0 )
          LODWORD(v67) = (_DWORD)v288;
        else
          v67 = (char *)&v287 + 1;
        sub_1F560(v259, (int)v67, v66);
        sub_364088(&v322, *(unsigned int *)(v33 + 196));
        if ( (v322 & 1) != 0 )
          v68 = *((_QWORD *)&v322 + 1);
        else
          v68 = (unsigned __int64)(unsigned __int8)v322 >> 1;
        if ( (v322 & 1) != 0 )
          LODWORD(v69) = v323;
        else
          v69 = (char *)&v322 + 1;
        sub_1F560(v258, (int)v69, v68);
        sub_2780C(v303, 7, v260, v259, v258, 0, 1);
        sub_2F3308((__int64)v303);
        if ( v321 >= 0x40u )
          sub_1F748(v320);
        if ( v319 >= 0x40u )
          sub_1F748(v318);
        if ( v317 >= 0x40u )
          sub_1F748(v316);
        if ( v315 >= 0x40u )
          sub_1F748(v313);
        if ( v312 >= 0x40u )
          sub_1F748(&v303[8]);
        if ( v258[23] >= 0x40u )
          sub_1F748(v258);
        if ( (v322 & 1) != 0 )
          sub_3684BC(v323);
        if ( v259[23] >= 0x40u )
          sub_1F748(v259);
        if ( (v287 & 1) != 0 )
          sub_3684BC(v288);
        if ( v260[23] >= 0x40u )
          sub_1F748(v260);
      }
      if ( v236 != *(_DWORD *)(v33 + 196) )
        goto LABEL_203;
      v70 = *(unsigned __int8 *)(v33 + 48);
      if ( (v264[0] & 1) != 0 )
        v71 = v264[1];
      else
        v71 = (unsigned __int64)LOBYTE(v264[0]) >> 1;
      if ( (v70 & 1) != 0 )
        v72 = *(_QWORD *)(v33 + 56);
      else
        v72 = v70 >> 1;
      if ( v71 == v72 )
      {
        if ( (v264[0] & 1) != 0 )
          v73 = v265;
        else
          v73 = (char *)v264 + 1;
        if ( (v70 & 1) != 0 )
          v74 = *(unsigned __int8 **)(v33 + 64);
        else
          v74 = (unsigned __int8 *)(v33 + 49);
        if ( (v264[0] & 1) == 0 )
        {
          if ( v71 )
          {
            v75 = -(__int64)((unsigned __int64)LOBYTE(v264[0]) >> 1);
            v76 = (unsigned __int8 *)v264 + 1;
            while ( *v76 == *v74 )
            {
              ++v76;
              ++v75;
              ++v74;
              if ( !v75 )
                goto LABEL_203;
            }
            goto LABEL_155;
          }
LABEL_203:
          sub_1E984(v303, v33 + 48);
          if ( (v289[0] & 1) != 0 )
            v87 = v290;
          else
            v87 = (unsigned __int64)(unsigned __int8)v289[0] >> 1;
          if ( (v289[0] & 1) != 0 )
            v88 = v291;
          else
            v88 = &v289[1];
          v89 = sub_2331C((int)v303, v88, v87);
          if ( (v296[0] & 1) != 0 )
            v90 = *(_QWORD *)&v296[8];
          else
            v90 = (unsigned __int64)(unsigned __int8)v296[0] >> 1;
          if ( v90 )
          {
            LODWORD(v322) = 30;
            WORD2(v322) = 24158;
            BYTE6(v322) = 0;
            for ( jj = 0; jj != 2; ++jj )
              *((_BYTE *)&v322 + jj + 4) ^= v322;
            BYTE6(v322) = 0;
            sub_5956C(&v287, (char *)&v322 + 4, v296);
            v92 = (v287 & 1) != 0 ? *((_QWORD *)&v287 + 1) : (unsigned __int64)(unsigned __int8)v287 >> 1;
            v93 = (v287 & 1) != 0 ? v288 : (char *)&v287 + 1;
            v89 = sub_2331C((int)v303, v93, v92);
            if ( (v287 & 1) != 0 )
              v89 = sub_3684BC(v288);
          }
          v94 = *(_DWORD *)(v33 + 200);
          if ( v94 > *(_DWORD *)(sub_26173C(v89) + 784) )
          {
            strcpy(src, "KKl~E");
            for ( kk = 0; kk != 5; ++kk )
              src[kk] -= 11;
            sub_364088(&v322, *(unsigned int *)(v33 + 200));
            v96 = strlen(src);
            v97 = sub_2840C((int)&v322, 0, src, v96);
            v288 = *(char **)(v97 + 16);
            v287 = *(_OWORD *)v97;
            *(_QWORD *)(v97 + 8) = 0;
            *(_QWORD *)(v97 + 16) = 0;
            *(_QWORD *)v97 = 0;
            if ( (v287 & 1) != 0 )
              v98 = *((_QWORD *)&v287 + 1);
            else
              v98 = (unsigned __int64)(unsigned __int8)v287 >> 1;
            if ( (v287 & 1) != 0 )
              v99 = v288;
            else
              v99 = (char *)&v287 + 1;
            sub_2331C((int)v303, v99, v98);
            if ( (v287 & 1) != 0 )
              sub_3684BC(v288);
            if ( (v322 & 1) != 0 )
              sub_3684BC(v323);
          }
          sub_241440(&v287, v303);
          sub_1E984(&v322, &v261);
          *(_DWORD *)src = 83;
          strcpy(&src[4], "{");
          v100 = strlen(&src[4]);
          sub_2331C((int)&v322, &src[4], v100);
          if ( (v287 & 1) != 0 )
            v101 = *((_QWORD *)&v287 + 1);
          else
            v101 = (unsigned __int64)(unsigned __int8)v287 >> 1;
          if ( (v287 & 1) != 0 )
            v102 = v288;
          else
            v102 = (char *)&v287 + 1;
          sub_2331C((int)&v322, v102, v101);
          *(_DWORD *)src = 101;
          strcpy(&src[4], "}");
          v103 = strlen(&src[4]);
          sub_2331C((int)&v322, &src[4], v103);
          if ( (v303[0] & 1) != 0 )
            v104 = *(_QWORD *)&v303[8];
          else
            v104 = (unsigned __int64)(unsigned __int8)v303[0] >> 1;
          if ( (v303[0] & 1) != 0 )
            v105 = (char *)v304;
          else
            v105 = &v303[1];
          v106 = sub_2331C((int)&v322, v105, v104);
          if ( v234 == 8 )
          {
            v28 = sub_26173C(v106);
            if ( *(_BYTE *)(v28 + 624) || v58 && (v28 = sub_26173C(v28), *(_BYTE *)(v28 + 674)) )
            {
              v107 = sub_102E24(v231);
              v28 = sub_310D4(v107);
            }
          }
          else
          {
            v108 = sub_102E24(v231);
            v28 = sub_310D4(v108);
            if ( v234 <= 0xA && ((1 << v234) & 0x4A9) != 0 )
              v28 = sub_301320(v28, v235, v33 + 24);
          }
          if ( (v322 & 1) != 0 )
            v28 = sub_3684BC(v323);
          if ( (v287 & 1) != 0 )
            v28 = sub_3684BC(v288);
          if ( (v303[0] & 1) != 0 )
            v28 = sub_3684BC(v304);
          goto LABEL_58;
        }
        if ( !v71 || !memcmp(v73, v74, v71) )
          goto LABEL_203;
      }
LABEL_155:
      strcpy((char *)&v322, "|pkfth{jo");
      for ( mm = 0; mm != 9; ++mm )
        *((_BYTE *)&v322 + mm) -= 7;
      sub_1E300(v257, &v322, src);
      sub_364088(&v287, v236);
      if ( (v287 & 1) != 0 )
        v78 = *((_QWORD *)&v287 + 1);
      else
        v78 = (unsigned __int64)(unsigned __int8)v287 >> 1;
      if ( (v287 & 1) != 0 )
        LODWORD(v79) = (_DWORD)v288;
      else
        v79 = (char *)&v287 + 1;
      sub_1F560(v256, (int)v79, v78);
      v80 = *(unsigned __int8 *)(v33 + 48);
      if ( (v80 & 1) != 0 )
        v81 = *(_QWORD *)(v33 + 64);
      else
        LODWORD(v81) = v33 + 49;
      if ( (v80 & 1) != 0 )
        v82 = *(_QWORD *)(v33 + 56);
      else
        v82 = v80 >> 1;
      sub_1F560(v255, v81, v82);
      if ( (v296[0] & 1) != 0 )
        v83 = *(_QWORD *)&v296[8];
      else
        v83 = (unsigned __int64)(unsigned __int8)v296[0] >> 1;
      if ( (v296[0] & 1) != 0 )
        LODWORD(v84) = (_DWORD)v297;
      else
        v84 = &v296[1];
      sub_1F560(v254, (int)v84, v83);
      if ( (v270 & 1) != 0 )
        v85 = v271;
      else
        v85 = (unsigned __int64)(unsigned __int8)v270 >> 1;
      if ( (v270 & 1) != 0 )
        LODWORD(v86) = v272;
      else
        v86 = (char *)&v270 + 1;
      sub_1F560(v253, (int)v86, v85);
      LOBYTE(v230) = 1;
      sub_2310C(v303, 7, v257, v256, v255, v254, v253, 0, v230, 0, 0);
      sub_2F3308((__int64)v303);
      if ( v321 >= 0x40u )
        sub_1F748(v320);
      if ( v319 >= 0x40u )
        sub_1F748(v318);
      if ( v317 >= 0x40u )
        sub_1F748(v316);
      if ( v315 >= 0x40u )
        sub_1F748(v313);
      if ( v312 >= 0x40u )
        sub_1F748(&v303[8]);
      if ( v253[23] >= 0x40u )
        sub_1F748(v253);
      if ( v254[23] >= 0x40u )
        sub_1F748(v254);
      if ( v255[23] >= 0x40u )
        sub_1F748(v255);
      if ( v256[23] >= 0x40u )
        sub_1F748(v256);
      if ( (v287 & 1) != 0 )
        sub_3684BC(v288);
      if ( v257[23] >= 0x40u )
        sub_1F748(v257);
      goto LABEL_203;
    }
LABEL_263:
    v109 = sub_276DDC(v28);
    v110 = sub_27BCA0(v109, &v268);
    v111 = sub_26F618(v110);
    v112 = v233;
    (*(void (__fastcall **)(char *__return_ptr, __int64, __int128 *))(*(_QWORD *)v111 + 96LL))(v303, v111, &v268);
    v113 = sub_102F24(a2 + 16, v303);
    if ( (v303[0] & 1) != 0 )
      v113 = sub_3684BC(v304);
    if ( a3 == 10 || a3 == 3 )
    {
      v114 = (char *)v266;
      if ( (_QWORD)v266 != *((_QWORD *)&v266 + 1) )
      {
        while ( 1 )
        {
          v115 = sub_26F618(v113);
          v113 = (*(__int64 (__fastcall **)(__int64, char *))(*(_QWORD *)v115 + 208LL))(v115, v114);
          if ( (v113 & 1) == 0 )
          {
            strcpy(v296, "lqjwpnmYjltjpn|");
            for ( nn = 0; nn != 15; ++nn )
              v296[nn] -= 9;
            memset(v303, 0, sizeof(v303));
            v304 = 0;
            v124 = strlen(v296);
            v125 = v124;
            if ( v124 >= 0xFFFFFFFFFFFFFFF0LL )
              sub_1EA30(v303);
            if ( v124 >= 0x17 )
            {
              v126 = (char *)sub_368454((v124 + 16) & 0xFFFFFFFFFFFFFFF0LL);
              v112 = v233;
              v304 = (unsigned __int64)v126;
              *(_QWORD *)v303 = (v125 + 16) & 0xFFFFFFFFFFFFFFF0LL | 1;
              *(_QWORD *)&v303[8] = v125;
            }
            else
            {
              v126 = &v303[1];
              v303[0] = 2 * v124;
              if ( !v124 )
                goto LABEL_286;
            }
            memcpy(v126, v296, v125);
LABEL_286:
            v126[v125] = 0;
            v113 = sub_3009BC(v112, v114, v303);
            if ( (v303[0] & 1) != 0 )
              v113 = sub_3684BC(v304);
            v118 = (char *)*((_QWORD *)&v266 + 1);
            v114 += 24;
            goto LABEL_295;
          }
          v116 = v114 + 24;
          if ( v114 + 24 == *((char **)&v266 + 1) )
          {
            v118 = v114;
          }
          else
          {
            v117 = *((_QWORD *)&v266 + 1) - 24LL;
            v118 = v114;
            do
            {
              v119 = v118;
              if ( (*v118 & 1) != 0 )
              {
                **((_BYTE **)v118 + 2) = 0;
                v120 = *v118;
                *((_QWORD *)v118 + 1) = 0;
                if ( (v120 & 1) != 0 )
                {
                  v113 = sub_3684BC(*((_QWORD *)v118 + 2));
                  *(_QWORD *)v118 = 0;
                }
              }
              else
              {
                *(_WORD *)v118 = 0;
              }
              v121 = *((_QWORD *)v118 + 5);
              v122 = *(_OWORD *)(v118 + 24);
              v118 += 24;
              *((_QWORD *)v119 + 3) = 0;
              *((_QWORD *)v119 + 4) = 0;
              *((_QWORD *)v119 + 2) = v121;
              *(_OWORD *)v119 = v122;
              *((_QWORD *)v119 + 5) = 0;
            }
            while ( (char *)v117 != v119 + 24 );
            v116 = (char *)*((_QWORD *)&v266 + 1);
            if ( *((char **)&v266 + 1) == v118 )
              goto LABEL_294;
          }
          v127 = v116;
          do
          {
            v128 = *(v127 - 24);
            v127 -= 24;
            if ( (v128 & 1) != 0 )
              v113 = sub_3684BC(*((_QWORD *)v116 - 1));
            v116 = v127;
          }
          while ( v118 != v127 );
LABEL_294:
          *((_QWORD *)&v266 + 1) = v118;
LABEL_295:
          if ( v118 == v114 )
          {
            a2 = v235;
            v129 = *(int **)(v235 + 104);
            if ( v129 )
            {
              v130 = *(int *)(v235 + 96);
              v131 = *v129;
              a3 = v234;
              if ( (int)v130 < *v129 )
              {
                *(_DWORD *)(v235 + 96) = v130 + 1;
                v132 = *(_QWORD *)&v129[2 * v130 + 2];
LABEL_303:
                strcpy(v303, "johunlkWhjrhnlz");
                for ( i1 = 0; i1 != 15; ++i1 )
                  v303[i1] -= 7;
                sub_2F9928(v132, v303);
                v137 = 0;
                strcpy(v289, "~~");
                do
                  v289[v137++] -= 2;
                while ( v137 != 2 );
                memset(v296, 0, sizeof(v296));
                v297 = 0;
                v138 = strlen(v289);
                v139 = v138;
                if ( v138 >= 0xFFFFFFFFFFFFFFF0LL )
                  sub_1EA30(v296);
                if ( v138 >= 0x17 )
                {
                  v141 = (v138 + 16) & 0xFFFFFFFFFFFFFFF0LL;
                  v140 = (char *)sub_368454(v141);
                  v297 = v140;
                  *(_QWORD *)v296 = v141 | 1;
                  *(_QWORD *)&v296[8] = v139;
                }
                else
                {
                  v140 = &v296[1];
                  v296[0] = 2 * v138;
                  if ( !v138 )
                  {
LABEL_313:
                    v140[v139] = 0;
                    sub_2E7DD4(v303, &v266, v296);
                    v142 = *(_QWORD *)(v132 + 8);
                    v143 = (__int64 **)(v132 + 24);
                    v144 = (_QWORD *)(v142 & 0xFFFFFFFFFFFFFFFELL);
                    if ( (v142 & 1) != 0 )
                      v144 = (_QWORD *)*v144;
                    if ( *v143 == &qword_4BFCC0 )
                      v113 = sub_7B87C(v143, v144, v303);
                    else
                      v113 = sub_310D4((int)*v143);
                    if ( (v303[0] & 1) != 0 )
                      v113 = sub_3684BC(v304);
                    if ( (v296[0] & 1) != 0 )
                      v113 = sub_3684BC(v297);
                    break;
                  }
                }
                memcpy(v140, v289, v139);
                goto LABEL_313;
              }
              if ( v131 == *(_DWORD *)(v235 + 100) )
              {
LABEL_301:
                sub_103074(v235 + 88, (unsigned int)(v131 + 1));
                v129 = *(int **)(v235 + 104);
                v131 = *v129;
              }
              *v129 = v131 + 1;
              v132 = sub_1F0F54(*(_QWORD *)(v235 + 88));
              v133 = *(int *)(v235 + 96);
              v134 = v133 + 1;
              v135 = *(_QWORD *)(v235 + 104) + 8 * v133;
              *(_DWORD *)(v235 + 96) = v134;
              *(_QWORD *)(v135 + 8) = v132;
              goto LABEL_303;
            }
            v131 = *(_DWORD *)(v235 + 100);
            a3 = v234;
            goto LABEL_301;
          }
        }
      }
    }
    v145 = sub_26173C(v113);
    if ( !*(_BYTE *)(v145 + 823) || a3 > 0xA || ((1 << a3) & 0x4A9) == 0 )
      goto LABEL_366;
    v322 = 0u;
    if ( (int)(v236 - 200) >= 10200 )
      v146 = 10200;
    else
      v146 = v236 - 200;
    if ( (int)(v236 + 200) <= 10500 )
      v147 = 10500;
    else
      v147 = v236 + 200;
    v323 = 0;
    v148 = sub_276DDC(v145);
    sub_27C00C(v303, v148);
    sub_305358(&v322);
    v149 = *(_OWORD *)v303;
    memset(v303, 0, sizeof(v303));
    v322 = v149;
    v323 = v304;
    v304 = 0;
    v150 = sub_264CAC(v303);
    v294 = 0;
    *(_QWORD *)src = 0;
    v295 = 0;
    v151 = sub_2650D8(v150);
    LODWORD(v287) = v146;
    if ( v146 > v147 )
    {
LABEL_332:
      v152 = *(_DWORD **)src;
      if ( v294 == *(_DWORD **)src )
      {
LABEL_363:
        if ( v152 )
        {
          v294 = v152;
          sub_3684BC(v152);
        }
        sub_264CAC(&v322);
LABEL_366:
        if ( (v261 & 1) != 0 )
          sub_3684BC(v263);
        if ( (v264[0] & 1) != 0 )
          sub_3684BC(v265);
        goto LABEL_370;
      }
      *(_DWORD *)v303 = 103;
      v303[4] = 72;
      v153 = 0;
      v303[5] = 27;
      v303[6] = 29;
      v303[7] = 5;
      v303[8] = 25;
      v303[9] = 13;
      v303[10] = 10;
      v303[11] = 11;
      v303[12] = 64;
      v303[13] = 21;
      v303[14] = 28;
      v303[15] = 7;
      v304 = 0x554957131301151FLL;
      v305 = 58;
      v306 = 18;
      v307 = 25;
      v308 = 12;
      v309 = 16;
      v310 = -23;
      v311 = -27;
      v312 = -83;
      v313[0] = -25;
      v313[1] = -27;
      v313[2] = -15;
      v313[3] = -25;
      v314 = 0;
      do
      {
        v303[v153 + 4] ^= (_BYTE)v153 + v303[0];
        ++v153;
      }
      while ( v153 != 32 );
      v314 = 0;
      v154 = sub_1E300(v251, &v303[4], v296);
      v250 = 23;
      v249[0] = 0;
      sub_12B534(&v287, v154);
      if ( (sub_1AE730(&v287) & 1) != 0 )
      {
LABEL_336:
        LODWORD(v285[0]) = 43;
        v155 = 0;
        strcpy((char *)v285 + 4, "FE^");
        do
        {
          *((_BYTE *)v285 + v155 + 4) ^= (_BYTE)v155 + LOBYTE(v285[0]);
          ++v155;
        }
        while ( v155 != 3 );
        HIBYTE(v285[0]) = 0;
        sub_1E300(v241, (char *)v285 + 4, &v282);
        sub_29014(v240, v249);
        sub_1E300(v239, &unk_39DD38, &v247);
        sub_1E300(v238, &unk_39DD38, &v280);
        sub_1E300(v237, &unk_39DD38, &v245);
        LOBYTE(v230) = 1;
        sub_2310C(v296, 2000, v241, v240, v239, v238, v237, 0, v230, ((__int64)v294 - *(_QWORD *)src) >> 3, 0);
        sub_2F3308((__int64)v296);
        if ( v302[23] >= 0x40u )
          sub_1F748(v302);
        if ( v301[23] >= 0x40u )
          sub_1F748(v301);
        if ( v300[23] >= 0x40u )
          sub_1F748(v300);
        if ( v299[23] >= 0x40u )
          sub_1F748(v299);
        if ( v298 >= 0x40u )
          sub_1F748(&v296[8]);
        if ( v237[23] >= 0x40u )
          sub_1F748(v237);
        if ( v238[23] >= 0x40u )
          sub_1F748(v238);
        if ( v239[23] >= 0x40u )
          sub_1F748(v239);
        if ( v240[23] >= 0x40u )
          sub_1F748(v240);
        if ( v241[23] >= 0x40u )
          sub_1F748(v241);
        sub_1B0760(&v287);
        if ( v250 >= 0x40u )
          sub_1F748(v249);
        if ( v252 >= 0x40u )
          sub_1F748(v251);
        v152 = *(_DWORD **)src;
        goto LABEL_363;
      }
      sub_131600(v296, &v287);
      LODWORD(v285[0]) = 12;
      strcpy((char *)v285 + 4, "/");
      if ( (v296[0] & 1) != 0 )
        v158 = v297;
      else
        v158 = &v296[1];
      if ( (v296[0] & 1) != 0 )
        v159 = *(_QWORD *)&v296[8];
      else
        v159 = (unsigned __int64)(unsigned __int8)v296[0] >> 1;
      v160 = strlen((const char *)v285 + 4);
      if ( v159 && v160 )
      {
        v161 = (unsigned __int8 *)&v158[v159];
        v162 = v158;
        while ( 2 )
        {
          v163 = v160;
          v164 = (unsigned __int8 *)v285 + 4;
          do
          {
            v165 = *v164++;
            if ( (unsigned __int8)*v162 == v165 )
            {
              if ( v162 != (char *)v161 && v162 - v158 != -1 )
              {
                sub_1FCA4(v285, v296, 0, v162 - v158, v296);
                v166 = 0;
                v167 = *(void (__fastcall **)(char *, __int64, char *, _QWORD *, char *))(qword_4C2298 + 776);
                v282 = 0x6D673B6D00000048LL;
                LOWORD(v283) = 59;
                do
                  *((_BYTE *)&v282 + v166++ + 4) ^= v282;
                while ( v166 != 5 );
                BYTE1(v283) = 0;
                if ( v252 >= 0x40u )
                  v168 = (_QWORD *)v251[0];
                else
                  v168 = v251;
                if ( (v285[0] & 1) != 0 )
                  v169 = v286;
                else
                  v169 = (char *)v285 + 1;
                v167(v303, 1024, (char *)&v282 + 4, v168, v169);
                if ( !(*(unsigned int (__fastcall **)(char *, char *))(qword_4C2298 + 232))(v303, v289) )
                {
                  v219 = *(_DWORD **)src;
                  v220 = v294;
                  if ( *(_DWORD **)src != v294 )
                  {
                    do
                    {
                      if ( *v219 == v292 )
                      {
                        strcpy(&v278[4], ":");
                        sub_23210((__int64 *)&v245, (int)v285, &v278[4]);
                        sub_364088(&v243, (unsigned int)*v219);
                        if ( (v243 & 1) != 0 )
                          v221 = v244;
                        else
                          v221 = (char *)&v243 + 1;
                        if ( (v243 & 1) != 0 )
                          v222 = *((_QWORD *)&v243 + 1);
                        else
                          v222 = (unsigned __int64)(unsigned __int8)v243 >> 1;
                        v223 = sub_2331C((int)&v245, v221, v222);
                        v281 = *(_QWORD *)(v223 + 16);
                        v280 = *(_OWORD *)v223;
                        *(_QWORD *)v223 = 0;
                        *(_QWORD *)(v223 + 8) = 0;
                        *(_QWORD *)(v223 + 16) = 0;
                        strcpy(&v274[4], ";");
                        v224 = strlen(&v274[4]);
                        v225 = sub_2331C((int)&v280, &v274[4], v224);
                        v248 = *(_OWORD **)(v225 + 16);
                        v247 = *(_OWORD *)v225;
                        *(_QWORD *)(v225 + 8) = 0;
                        *(_QWORD *)(v225 + 16) = 0;
                        *(_QWORD *)v225 = 0;
                        if ( (v247 & 1) != 0 )
                          LODWORD(v226) = (_DWORD)v248;
                        else
                          v226 = (char *)&v247 + 1;
                        if ( (v247 & 1) != 0 )
                          v227 = *((_QWORD *)&v247 + 1);
                        else
                          v227 = (unsigned __int64)(unsigned __int8)v247 >> 1;
                        sub_1F560(&v282, (int)v226, v227);
                        if ( v284 >= 0x40uLL )
                          v228 = (__int64 *)v282;
                        else
                          v228 = &v282;
                        if ( 23LL - v284 < 0 )
                          v229 = v283;
                        else
                          v229 = 23LL - v284;
                        sub_376D0(v249, v228, v229);
                        if ( v284 >= 0x40u )
                          sub_1F748(&v282);
                        if ( (v247 & 1) != 0 )
                          sub_3684BC(v248);
                        if ( (v280 & 1) != 0 )
                          sub_3684BC(v281);
                        if ( (v243 & 1) != 0 )
                          sub_3684BC(v244);
                        if ( (v245 & 1) != 0 )
                          sub_3684BC(v246);
                      }
                      v219 += 2;
                    }
                    while ( v220 != v219 );
                  }
                }
                if ( (v285[0] & 1) != 0 )
                  sub_3684BC(v286);
              }
              goto LABEL_401;
            }
            --v163;
          }
          while ( v163 );
          if ( ++v162 != (char *)v161 )
            continue;
          break;
        }
      }
LABEL_401:
      v242 = 0;
      sub_12CD5C(v285, &v287, &v242);
      LODWORD(v280) = 88;
      WORD2(v280) = 58;
      v248 = 0;
      v247 = 0u;
      v170 = strlen((const char *)&v280 + 4);
      v171 = v170;
      if ( v170 >= 0xFFFFFFFFFFFFFFF0LL )
        sub_1EA30(&v247);
      if ( v170 >= 0x17 )
      {
        v173 = (v170 + 16) & 0xFFFFFFFFFFFFFFF0LL;
        v172 = (char *)sub_368454(v173);
        *((_QWORD *)&v247 + 1) = v171;
        v248 = v172;
        *(_QWORD *)&v247 = v173 | 1;
      }
      else
      {
        v172 = (char *)&v247 + 1;
        LOBYTE(v247) = 2 * v170;
        if ( !v170 )
          goto LABEL_407;
      }
      memcpy(v172, (char *)&v280 + 4, v171);
LABEL_407:
      v172[v171] = 0;
      sub_2E7B8C(&v282, v285, &v247);
      if ( (v247 & 1) != 0 )
        sub_3684BC(v248);
      v174 = v282;
      for ( i2 = v283; v174 != i2; v174 += 24 )
      {
        v187 = sub_1D928(v174, 47, 0);
        if ( v187 != -1 )
        {
          sub_1FCA4(&v247, v174, 0, v187, v174);
          v188 = v247;
          v189 = *((_QWORD *)&v247 + 1);
          if ( (v247 & 1) == 0 )
            v189 = (unsigned __int64)(unsigned __int8)v247 >> 1;
          if ( v189 )
          {
            v190 = 0;
            v191 = *(void (__fastcall **)(char *, __int64, char *, _QWORD *, _OWORD *))(qword_4C2298 + 776);
            *(_QWORD *)&v280 = 0x60C500600000023LL;
            WORD4(v280) = 80;
            do
              *((_BYTE *)&v280 + v190++ + 4) ^= v280;
            while ( v190 != 5 );
            BYTE9(v280) = 0;
            v192 = v252 >= 0x40u ? (_QWORD *)v251[0] : v251;
            v193 = (v188 & 1) != 0 ? v248 : (__int128 *)((char *)&v247 + 1);
            v191(v303, 1024, (char *)&v280 + 4, v192, v193);
            if ( !(*(unsigned int (__fastcall **)(char *, char *))(qword_4C2298 + 232))(v303, v289) )
            {
              v176 = *(_DWORD **)src;
              v177 = v294;
              if ( *(_DWORD **)src != v294 )
              {
                do
                {
                  if ( *v176 == v292 )
                  {
                    v277 = 12;
                    strcpy(v278, ":");
                    sub_23210((__int64 *)&v278[4], (int)&v247, v278);
                    sub_364088(&v274[4], (unsigned int)*v176);
                    if ( (v274[4] & 1) != 0 )
                      v178 = v276;
                    else
                      v178 = &v274[5];
                    if ( (v274[4] & 1) != 0 )
                      v179 = v275;
                    else
                      v179 = (unsigned __int64)(unsigned __int8)v274[4] >> 1;
                    v180 = sub_2331C((int)&v278[4], v178, v179);
                    v244 = *(char **)(v180 + 16);
                    v243 = *(_OWORD *)v180;
                    *(_QWORD *)v180 = 0;
                    *(_QWORD *)(v180 + 8) = 0;
                    *(_QWORD *)(v180 + 16) = 0;
                    v273 = 89;
                    strcpy(v274, ";");
                    v181 = strlen(v274);
                    v182 = sub_2331C((int)&v243, v274, v181);
                    v246 = *(_QWORD *)(v182 + 16);
                    v245 = *(_OWORD *)v182;
                    *(_QWORD *)(v182 + 8) = 0;
                    *(_QWORD *)(v182 + 16) = 0;
                    *(_QWORD *)v182 = 0;
                    if ( (v245 & 1) != 0 )
                      v183 = *((_QWORD *)&v245 + 1);
                    else
                      v183 = (unsigned __int64)(unsigned __int8)v245 >> 1;
                    if ( (v245 & 1) != 0 )
                      LODWORD(v184) = v246;
                    else
                      v184 = (char *)&v245 + 1;
                    sub_1F560(&v280, (int)v184, v183);
                    if ( HIBYTE(v281) >= 0x40uLL )
                      v185 = (__int128 *)v280;
                    else
                      v185 = &v280;
                    if ( 23LL - HIBYTE(v281) < 0 )
                      v186 = *((_QWORD *)&v280 + 1);
                    else
                      v186 = 23LL - HIBYTE(v281);
                    sub_376D0(v249, v185, v186);
                    if ( HIBYTE(v281) >= 0x40u )
                      sub_1F748(&v280);
                    if ( (v245 & 1) != 0 )
                      sub_3684BC(v246);
                    if ( (v243 & 1) != 0 )
                      sub_3684BC(v244);
                    if ( (v274[4] & 1) != 0 )
                      sub_3684BC(v276);
                    if ( (v278[4] & 1) != 0 )
                      sub_3684BC(v279);
                  }
                  v176 += 2;
                }
                while ( v177 != v176 );
              }
            }
          }
          if ( (v247 & 1) != 0 )
            sub_3684BC(v248);
        }
      }
      sub_301BC(&v282);
      if ( (v285[0] & 1) != 0 )
        sub_3684BC(v286);
      if ( (v296[0] & 1) != 0 )
        sub_3684BC(v297);
      goto LABEL_336;
    }
    v157 = (_QWORD *)(a2 + 160);
    while ( 1 )
    {
      v198 = v236;
      if ( v146 != v236 )
        break;
LABEL_518:
      v146 = v198 + 1;
      LODWORD(v287) = v198 + 1;
      if ( v198 >= v147 )
        goto LABEL_332;
    }
    v199 = sub_2A837C(v151);
    v200 = (*(_QWORD *(__fastcall **)(char *__return_ptr, __int64, _QWORD))(*(_QWORD *)v199 + 96LL))(
             v303,
             v199,
             (unsigned int)v287);
    v151 = sub_3029B0(v200, v303);
    if ( (v151 & 1) != 0 || (v151 = sub_2EA8A0((unsigned int)v287)) == 0 )
    {
LABEL_515:
      if ( (v303[0] & 1) != 0 )
        v151 = sub_3684BC(v304);
      v198 = v287;
      goto LABEL_518;
    }
    LODWORD(v251[0]) = 2000;
    while ( 1 )
    {
      v151 = sub_2EA9F8((unsigned int)v287);
      if ( v151 )
        break;
      if ( LODWORD(v251[0])++ >= 30000 )
        goto LABEL_515;
    }
    v202 = *(int **)(v235 + 176);
    if ( v202 )
    {
      v203 = *(int *)(v235 + 168);
      v204 = *v202;
      if ( (int)v203 < *v202 )
      {
        *(_DWORD *)(v235 + 168) = v203 + 1;
        v205 = *(_QWORD *)&v202[2 * v203 + 2];
LABEL_484:
        v195 = *(_QWORD *)&v303[8];
        v197 = (unsigned __int64)(unsigned __int8)v303[0] >> 1;
        if ( (v303[0] & 1) != 0 )
          v209 = *(_QWORD *)&v303[8];
        else
          v209 = (unsigned __int64)(unsigned __int8)v303[0] >> 1;
        v196 = v303[0] & 1;
        if ( !v209 )
        {
          v210 = v322;
          if ( (_QWORD)v322 != *((_QWORD *)&v322 + 1) )
          {
            while ( *(_DWORD *)(v210 + 24) != (_DWORD)v287 )
            {
              v210 += 32;
              if ( *((_QWORD *)&v322 + 1) == v210 )
                goto LABEL_488;
            }
            strcpy(v289, "[X;");
            for ( i3 = 0; i3 != 3; ++i3 )
              --v289[i3];
            sub_5956C(v296, v289, v210);
            if ( (v303[0] & 1) != 0 )
            {
              *(_BYTE *)v304 = 0;
              *(_QWORD *)&v303[8] = 0;
              if ( (v303[0] & 1) != 0 )
              {
                sub_3684BC(v304);
                *(_QWORD *)v303 = 0;
              }
            }
            else
            {
              *(_WORD *)v303 = 0;
            }
            *(_OWORD *)v303 = *(_OWORD *)v296;
            v304 = (unsigned __int64)v297;
            v195 = *(_QWORD *)&v296[8];
            v196 = v296[0] & 1;
            v197 = (unsigned __int64)(unsigned __int8)v296[0] >> 1;
          }
        }
LABEL_488:
        if ( !v196 )
          v195 = v197;
        if ( v195 )
        {
          *(_DWORD *)(v205 + 32) = v251[0];
        }
        else
        {
          *(_DWORD *)(v205 + 32) = 99;
          v211 = v294;
          if ( (unsigned __int64)v294 >= v295 )
          {
            sub_3053CC(src, &v287, v251);
          }
          else
          {
            *v294 = v287;
            v211[1] = v251[0];
            v294 = v211 + 2;
          }
        }
        sub_364088(v296, (unsigned int)v287);
        v212 = *(_QWORD *)(v205 + 8);
        v213 = (__int64 **)(v205 + 16);
        v214 = (_QWORD *)(v212 & 0xFFFFFFFFFFFFFFFELL);
        if ( (v212 & 1) != 0 )
          v214 = (_QWORD *)*v214;
        if ( *v213 == &qword_4BFCC0 )
          sub_7B87C(v213, v214, v296);
        else
          sub_310D4((int)*v213);
        if ( (v296[0] & 1) != 0 )
          sub_3684BC(v297);
        v215 = 0;
        strcpy(v289, "tmh>");
        do
          v289[v215++] -= 4;
        while ( v215 != 4 );
        sub_5956C(v296, v289, v303);
        v216 = *(_QWORD *)(v205 + 8);
        v217 = (__int64 **)(v205 + 24);
        v218 = (_QWORD *)(v216 & 0xFFFFFFFFFFFFFFFELL);
        if ( (v216 & 1) != 0 )
          v218 = (_QWORD *)*v218;
        if ( *v217 == &qword_4BFCC0 )
          v151 = sub_7B87C(v217, v218, v296);
        else
          v151 = sub_310D4((int)*v217);
        if ( (v296[0] & 1) != 0 )
          v151 = sub_3684BC(v297);
        goto LABEL_515;
      }
      if ( v204 != *(_DWORD *)(v235 + 172) )
      {
LABEL_483:
        *v202 = v204 + 1;
        v205 = sub_1F1144(*v157);
        v206 = *(int *)(v235 + 168);
        v207 = v206 + 1;
        v208 = *(_QWORD *)(v235 + 176) + 8 * v206;
        *(_DWORD *)(v235 + 168) = v207;
        *(_QWORD *)(v208 + 8) = v205;
        goto LABEL_484;
      }
    }
    else
    {
      v204 = *(_DWORD *)(v235 + 172);
    }
    sub_103074(v157, (unsigned int)(v204 + 1));
    v202 = *(int **)(v235 + 176);
    v204 = *v202;
    goto LABEL_483;
  }
LABEL_370:
  sub_301BC(&v266);
  result = sub_409AC(&v268);
  if ( (v270 & 1) != 0 )
    return sub_3684BC(v272);
  return result;
}
