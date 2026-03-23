<template>
    <div class="relative group">
        <div ref="plotEl" style="height: 350px"></div>
        <div
            class="absolute top-2 right-2 flex gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity duration-150 z-10"
        >
            <button
                class="h-7 w-7 flex items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground transition-colors"
                title="Zoom In"
                @click="handleZoomIn"
            >
                <ZoomIn class="size-4" />
            </button>
            <button
                class="h-7 w-7 flex items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground transition-colors"
                title="Zoom Out"
                @click="handleZoomOut"
            >
                <ZoomOut class="size-4" />
            </button>
            <button
                class="h-7 w-7 flex items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground transition-colors"
                title="Reset View"
                @click="handleReset"
            >
                <RotateCcw class="size-3.5" />
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { ZoomIn, ZoomOut, RotateCcw } from "lucide-vue-next";
import { newPlot, purge, relayout, type Data, type Layout, type PlotlyHTMLElement } from "plotly.js-dist";
import { useColorMode } from "@vueuse/core";

const props = defineProps<{
    json: string;
}>();

const plotEl = ref<HTMLElement | null>(null);
const theme = useColorMode();
const plotlyTheme = computed(() => {
    const mutedForeground = theme.value === "dark" ? "#6a7282" : "#99a1af";
    const foreground = theme.value === "dark" ? "#f9fafb" : "#030712"
    return {
        font: {
            color: foreground
        },
        xaxis: {
            gridcolor: mutedForeground,
            zerolinecolor: foreground,
        },
        yaxis: {
            gridcolor: mutedForeground,
            zerolinecolor: foreground,
        }
    };
});

async function mountPlot() {
    const el = plotEl.value;
    if (!el) {
        return;
    }

    try {
        const config = JSON.parse(props.json) as {
            data: Data[];
            layout: Layout;
        };

        const layout: Layout = {
            ...config.layout,
            plot_bgcolor: "transparent",
            paper_bgcolor: "transparent",
            ...plotlyTheme.value
        };

        await newPlot(el, config.data, layout, {
            responsive: true,
            displayModeBar: false
        });
    } catch {
        if (plotEl.value) {
            plotEl.value.textContent = "Failed to render chart";
        }
    }
}

type PlotlyGd = { _fullLayout?: { xaxis?: { range?: [number, number] }; yaxis?: { range?: [number, number] } } };

async function handleZoomIn() {
    const el = plotEl.value;
    if (el == undefined) {
        return;
    }
    const gd = el as unknown as PlotlyGd;
    const xRange = gd._fullLayout?.xaxis?.range;
    const yRange = gd._fullLayout?.yaxis?.range;
    if (!xRange || !yRange) {
        return;
    }
    const xCenter = (xRange[0] + xRange[1]) / 2;
    const yCenter = (yRange[0] + yRange[1]) / 2;
    relayout(el as PlotlyHTMLElement, {
        "xaxis.range": [xCenter - (xRange[1] - xRange[0]) * 0.375, xCenter + (xRange[1] - xRange[0]) * 0.375],
        "yaxis.range": [yCenter - (yRange[1] - yRange[0]) * 0.375, yCenter + (yRange[1] - yRange[0]) * 0.375]
    });
}

async function handleZoomOut() {
    const el = plotEl.value;
    if (el == undefined) {
        return;
    }
    const gd = el as unknown as PlotlyGd;
    const xRange = gd._fullLayout?.xaxis?.range;
    const yRange = gd._fullLayout?.yaxis?.range;
    if (!xRange || !yRange) {
        return;
    }
    const xCenter = (xRange[0] + xRange[1]) / 2;
    const yCenter = (yRange[0] + yRange[1]) / 2;
    relayout(el as PlotlyHTMLElement, {
        "xaxis.range": [xCenter - (xRange[1] - xRange[0]) * 0.75, xCenter + (xRange[1] - xRange[0]) * 0.75],
        "yaxis.range": [yCenter - (yRange[1] - yRange[0]) * 0.75, yCenter + (yRange[1] - yRange[0]) * 0.75]
    });
}

async function handleReset() {
    const el = plotEl.value;
    if (!el) {
        return;
    }
    relayout(el as PlotlyHTMLElement, {
        "xaxis.autorange": true,
        "yaxis.autorange": true
    });
}

watch(theme, () => {
    const el = plotEl.value;
    if (el == undefined) {
        return;
    }
    relayout(el as PlotlyHTMLElement, {
        ...plotlyTheme.value
    });
});

onMounted(() => {
    mountPlot();
});

onUnmounted(() => {
    const el = plotEl.value;
    if (el != undefined) {
        purge(el);
    }
});
</script>
