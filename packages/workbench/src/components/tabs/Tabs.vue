<template>
  <TabsRoot v-if="tabs.length > 0" v-model="activeTabPath">
    <TabsList class="text-muted-foreground inline-flex w-fit items-center justify-start rounded-lg gap-1 m-1.5">
      <FileTab
        v-for="tab in tabs"
        :key="tab.file.path"
        :tab="tab"
        :is-active="activeTab === tab"
        @close="closeTab"
      />
    </TabsList>
  </TabsRoot>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { TabsRoot, TabsList } from 'reka-ui'
import { workbenchStateKey } from '@/data/workbenchState'
import FileTab from './FileTab.vue'
import type { EditorTab } from '@/data/tab/editorTab'

const workbenchState = inject(workbenchStateKey)!

const tabs = computed(() => workbenchState.value.tabs.value)
const activeTab = computed(() => workbenchState.value.activeTab.value)

const activeTabPath = computed({
  get: () => activeTab.value?.file.path ?? '',
  set: (path: string) => {
    const tab = tabs.value.find(t => t.file.path === path)
    if (tab) {
      workbenchState.value.activeTab.value = tab
    }
  }
})

const closeTab = (tab: EditorTab) => {
  const currentTabs = tabs.value
  const index = currentTabs.findIndex(t => t === tab)
  
  if (index === -1) return
  
  // Remove the tab
  workbenchState.value.tabs.value = currentTabs.filter(t => t !== tab)
  
  // If this was the active tab, select another one
  if (activeTab.value === tab) {
    const remainingTabs = workbenchState.value.tabs.value
    if (remainingTabs.length > 0) {
      // Select the tab to the right, or the rightmost tab if we closed the last one
      const newIndex = Math.min(index, remainingTabs.length - 1)
      workbenchState.value.activeTab.value = remainingTabs[newIndex]
    } else {
      workbenchState.value.activeTab.value = undefined
    }
  }
}
</script>
