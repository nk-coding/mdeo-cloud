<template>
    <svg viewBox="-75 -75 150 150">
        <defs>
            <linearGradient :id="gradientId" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color: #0078d4; stop-opacity: 1" />
                <stop offset="100%" style="stop-color: #00b4d8; stop-opacity: 1" />
            </linearGradient>
            <mask :id="maskId">
                <g transform="translate(-26, 0) rotate(0)">
                    <rect stroke="none" fill="white" x="-15" y="-11.5" width="30" height="23" rx="5" />
                    <path
                        v-if="showText"
                        fill="black"
                        d="M267 1456L521 1456L895 387L1269 1456L1523 1456L997 0L793 0ZM130 1456L383 1456L429 414L429 0L130 0ZM1407 1456L1661 1456L1661 0L1361 0L1361 414Z"
                        transform="translate(-7.00390625, 5.46875) scale(0.0078125, -0.0078125)"
                    />
                </g>
                <g transform="translate(0, -26) rotate(0)">
                    <rect stroke="none" fill="white" x="-15" y="-11.5" width="30" height="23" rx="5" />
                    <path
                        v-if="showText"
                        fill="black"
                        d="M577 0L260 0L262 234L577 234Q695 234 776 287.5Q857 341 898.5 444Q940 547 940 693L940 764Q940 876 916.5 961Q893 1046 847 1104Q801 1162 734 1191.5Q667 1221 580 1221L254 1221L254 1456L580 1456Q726 1456 847.5 1406.5Q969 1357 1058 1265Q1147 1173 1195.5 1045.5Q1244 918 1244 762L1244 693Q1244 538 1195.5 410Q1147 282 1058.5 190.5Q970 99 847.5 49.5Q725 0 577 0ZM430 1456L430 0L130 0L130 1456Z"
                        transform="translate(-5.19921875, 5.46875) scale(0.0078125, -0.0078125)"
                    />
                </g>
                <g transform="translate(26, 0) rotate(0)">
                    <rect stroke="none" fill="white" x="-15" y="-11.5" width="30" height="23" rx="5" />
                    <path
                        v-if="showText"
                        fill="black"
                        d="M1107 234L1107 0L332 0L332 234ZM430 1456L430 0L130 0L130 1456ZM1006 863L1006 635L332 635L332 863ZM1106 1456L1106 1221L332 1221L332 1456Z"
                        transform="translate(-4.5, 5.46875) scale(0.0078125, -0.0078125)"
                    />
                </g>
                <g transform="translate(0, 26) rotate(0)">
                    <rect stroke="none" fill="white" x="-15" y="-11.5" width="30" height="23" rx="5" />
                    <path
                        v-if="showText"
                        fill="black"
                        d="M1326 762L1326 693Q1326 527 1281 395Q1236 263 1154 170.5Q1072 78 958.5 29Q845 -20 707 -20Q570 -20 456.5 29Q343 78 260 170.5Q177 263 131.5 395Q86 527 86 693L86 762Q86 929 131 1060.5Q176 1192 258.5 1285Q341 1378 454.5 1427Q568 1476 705 1476Q843 1476 956.5 1427Q1070 1378 1152.5 1285Q1235 1192 1280.5 1060.5Q1326 929 1326 762ZM1023 693L1023 764Q1023 880 1002 968Q981 1056 940 1116Q899 1176 840 1206.5Q781 1237 705 1237Q629 1237 570.5 1206.5Q512 1176 471.5 1116Q431 1056 410 968Q389 880 389 764L389 693Q389 578 410 489.5Q431 401 472 340.5Q513 280 572 249Q631 218 707 218Q783 218 842 249Q901 280 941 340.5Q981 401 1002 489.5Q1023 578 1023 693Z"
                        transform="translate(-5.515625, 5.46875) scale(0.0078125, -0.0078125)"
                    />
                </g>
                <g transform="translate(0, 0) rotate(0)">
                    <path
                        stroke="white"
                        stroke-width="5"
                        stroke-linecap="butt"
                        stroke-linejoin="miter"
                        stroke-miterlimit="4"
                        fill="none"
                        d="M0 14.75L8.1125 14.75 14.75 8.1125 14.75 0 14.75 8.1125 21.3875 14.75 29.5 14.75 21.3875 14.75 14.75 21.3875 14.75 29.5 14.75 21.3875 8.1125 14.75Z"
                        transform="translate(-14.75, -14.75)"
                    />
                </g>
                <g transform="translate(0, 0) rotate(0)">
                    <path
                        stroke="white"
                        stroke-width="5"
                        stroke-linecap="butt"
                        stroke-linejoin="miter"
                        stroke-miterlimit="4"
                        fill="none"
                        d="M43.227000000000004 2.5L43.487 2.5 54.5 13.513 54.5 17.26M54.5 39.739999999999995L54.5 43.487 43.487 54.5 43.227000000000004 54.5M13.773 54.5L13.513 54.5 2.5 43.487 2.5 39.739999999999995M2.5 17.26L2.5 13.513 13.513 2.5 13.773 2.5"
                        transform="translate(-28.5, -28.5)"
                    />
                </g>
                <g transform="translate(0, 0) rotate(0)">
                    <path
                        stroke="white"
                        stroke-width="10"
                        stroke-linecap="butt"
                        stroke-linejoin="round"
                        stroke-miterlimit="4"
                        fill="none"
                        d="M 51.625 12.773874705820468L 59 0L 29.49999999999996 -51.0954988232819L -3.197442310920451e-14 -51.095498823281886C -26.000000000000032 -51.095498823281886 -44.250000000000014 -49.54774941164093 -44.250000000000014 -25.54774941164093L -44.250000000000014 -25.34774941164093"
                        transform="translate(0, 0)"
                    />
                </g>
                <g transform="translate(-44.250000000000014, -14.547749411640929) rotate(90)">
                    <path
                        stroke="white"
                        stroke-width="2"
                        stroke-linecap="butt"
                        stroke-linejoin="round"
                        stroke-miterlimit="4"
                        fill="white"
                        d="M1 1L10 8.75 1 16.5Z"
                        transform="translate(-11, -8.75)"
                    />
                </g>
                <g transform="translate(0, 0) rotate(0)">
                    <path
                        stroke="white"
                        stroke-width="10"
                        stroke-linecap="butt"
                        stroke-linejoin="round"
                        stroke-miterlimit="4"
                        fill="none"
                        d="M -51.62500000000001 -12.773874705820461L -59 7.225416114969383e-15L -29.499999999999986 51.095498823281886L 1.0658141036401503e-14 51.095498823281886C 26.00000000000001 51.095498823281886 44.25 49.547749411640936 44.25 25.54774941164094L 44.25 25.34774941164094"
                        transform="translate(0, 0)"
                    />
                </g>
                <g transform="translate(44.25, 14.54774941164094) rotate(-90)">
                    <path
                        stroke="white"
                        stroke-width="2"
                        stroke-linecap="butt"
                        stroke-linejoin="round"
                        stroke-miterlimit="4"
                        fill="white"
                        d="M1 1L10 8.75 1 16.5Z"
                        transform="translate(-11, -8.75)"
                    />
                </g>
            </mask>
        </defs>
        <rect
            x="-75"
            y="-75"
            width="150"
            height="150"
            :fill="solid ? 'var(--color-muted)' : `url(#${gradientId})`"
            :opacity="solid ? 0.8 : 1"
            :mask="`url(#${maskId})`"
        />
    </svg>
</template>
<script setup lang="ts">
import { useId } from "vue";

defineProps<{
    showText?: boolean;
    solid?: boolean;
}>();

const maskId = useId();
const gradientId = useId();
</script>
