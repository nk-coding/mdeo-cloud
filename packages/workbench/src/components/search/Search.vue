<template>
    <div class="px-3 pt-2">
        <Input v-model="serachText" placeholder="Search..." />
    </div>
</template>
<script setup lang="ts">
import { inject, onMounted, ref, shallowRef } from "vue";
import { Input } from "../ui/input";
import { monacoApiProviderKey } from "@/plugins/monacoPlugin";
import { getService, ISearchService } from "@codingame/monaco-vscode-api";
import { throttledWatch } from "@vueuse/core";
import { QueryType } from "@codingame/monaco-vscode-api/vscode/vs/workbench/services/search/common/search";
import { Uri } from "vscode";

const monaco = inject(monacoApiProviderKey)!;

const api = shallowRef<{ serachService: ISearchService; monaco: Awaited<typeof monaco> }>();
const serachText = ref("");

throttledWatch(
    serachText,
    async (newValue) => {
        console.log("searching for", newValue);
        if (api.value != undefined) {
            const { serachService, monaco } = api.value;
            serachService.clearCache("");
            const serachResult = await serachService.textSearch({
                type: QueryType.Text,
                contentPattern: {
                    pattern: newValue
                },
                folderQueries: [
                    {
                        folder: Uri.parse("file:///")
                    }
                ]
            });
            console.log(serachResult);
        }
    },
    { throttle: 300, leading: true, trailing: true }
);

onMounted(async () => {
    await monaco;
    api.value = {
        serachService: await getService(ISearchService),
        monaco: await monaco
    };
});
</script>
