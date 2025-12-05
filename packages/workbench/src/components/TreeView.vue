<template>
  <div
    ref="treeContainer"
    class="tree-view focus:outline-none"
    tabindex="0"
    @keydown="handleKeyDown"
  >
    <TreeViewItem
      v-for="item in items"
      :key="item.id"
      :item="item"
      :active-id="activeId"
      :focused-id="focusedId"
      :depth="0"
      @toggle="handleToggle"
      @activate="handleActivate"
      @focus-change="handleFocusChange"
      @start-edit="handleStartEdit"
      @save-edit="handleSaveEdit"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, nextTick, provide } from 'vue'
import { useFocusWithin } from '@vueuse/core'
import type { FileSystemEntry } from '@/lib/filesystem'
import TreeViewItem from './TreeViewItem.vue'

/**
 * Props for the TreeView component
 */
interface TreeViewProps {
  /** Array of root-level items to display */
  items: FileSystemEntry[]
  /** ID of the initially active item */
  defaultActiveId?: string
}

/**
 * Events emitted by the TreeView component
 */
interface TreeViewEmits {
  /** Emitted when an item is activated */
  activate: [item: FileSystemEntry]
  /** Emitted when an item is renamed */
  rename: [{ item: FileSystemEntry; newName: string }]
}

const props = defineProps<TreeViewProps>()
const emit = defineEmits<TreeViewEmits>()

// Template refs
const treeContainer = ref<HTMLElement>()

// Internal state
const activeId = ref<string | undefined>(props.defaultActiveId)
const focusedId = ref<string | undefined>()
const expandedItems = ref(new Set<string>())

// Focus management
const { focused: isTreeFocused } = useFocusWithin(treeContainer)

// Provide expanded state to child components
provide('expandedItems', expandedItems)
provide('isItemExpanded', (id: string) => expandedItems.value.has(id))

// Get all focusable items in order
const allItems = computed(() => {
  const items: FileSystemEntry[] = []
  
  function collectItems(itemList: FileSystemEntry[]): void {
    for (const item of itemList) {
      items.push(item)
      if (item.type === 'folder' && expandedItems.value.has(item.id) && item.children) {
        collectItems(item.children)
      }
    }
  }
  
  collectItems(props.items)
  return items
})

/**
 * Handles toggle events from tree items
 */
function handleToggle(itemId: string): void {
  if (expandedItems.value.has(itemId)) {
    expandedItems.value.delete(itemId)
  } else {
    expandedItems.value.add(itemId)
  }
}

/**
 * Handles activation events from tree items
 */
function handleActivate(itemId: string): void {
  activeId.value = itemId
  const item = findItemById(itemId)
  if (item) {
    emit('activate', item)
  }
}

/**
 * Handles focus change events from tree items
 */
function handleFocusChange(itemId: string | null): void {
  focusedId.value = itemId || undefined
}

/**
 * Handles start edit events from tree items
 */
function handleStartEdit(itemId: string): void {
  // Focus management for editing is handled in TreeViewItem
}

/**
 * Handles save edit events from tree items
 */
function handleSaveEdit({ id, newName }: { id: string; newName: string }): void {
  const item = findItemById(id)
  if (item) {
    emit('rename', { item, newName })
  }
}

/**
 * Handles keyboard navigation for the entire tree
 */
function handleKeyDown(event: KeyboardEvent): void {
  if (!isTreeFocused.value) return
  
  const items = allItems.value
  const currentIndex = focusedId.value ? items.findIndex(item => item.id === focusedId.value) : -1
  
  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      navigateToItem(currentIndex + 1)
      break
    case 'ArrowUp':
      event.preventDefault()
      navigateToItem(currentIndex - 1)
      break
    case 'Home':
      event.preventDefault()
      navigateToItem(0)
      break
    case 'End':
      event.preventDefault()
      navigateToItem(items.length - 1)
      break
    case 'Enter':
      if (focusedId.value) {
        handleActivate(focusedId.value)
      }
      break
  }
}

/**
 * Navigates to a specific item by index
 */
function navigateToItem(index: number): void {
  const items = allItems.value
  if (index >= 0 && index < items.length) {
    const item = items[index]
    if (item) {
      focusedId.value = item.id
      
      // Focus the specific tree item
      nextTick(() => {
        const itemElement = treeContainer.value?.querySelector(`[tabindex="0"]`) as HTMLElement
        itemElement?.focus()
      })
    }
  }
}

/**
 * Finds an item by its ID recursively
 */
function findItemById(id: string): FileSystemEntry | undefined {
  function search(items: FileSystemEntry[]): FileSystemEntry | undefined {
    for (const item of items) {
      if (item.id === id) return item
      if (item.children) {
        const found = search(item.children)
        if (found) return found
      }
    }
    return undefined
  }
  
  return search(props.items)
}

/**
 * Expands all folders up to a specific item
 */
function expandToItem(itemId: string): void {
  const item = findItemById(itemId)
  if (!item) return
  
  // Find path to root and expand all folders
  const path: string[] = []
  let current = item
  
  while (current.parentId) {
    path.unshift(current.parentId)
    current = findItemById(current.parentId)!
  }
  
  for (const folderId of path) {
    expandedItems.value.add(folderId)
  }
}

// Expose methods for external control
defineExpose({
  /**
   * Sets the active item
   */
  setActiveItem: (itemId: string) => {
    activeId.value = itemId
    expandToItem(itemId)
  },
  
  /**
   * Focuses a specific item
   */
  focusItem: (itemId: string) => {
    focusedId.value = itemId
    expandToItem(itemId)
    
    nextTick(() => {
      const itemElement = treeContainer.value?.querySelector(`[tabindex="0"]`) as HTMLElement
      itemElement?.focus()
    })
  },
  
  /**
   * Gets the currently active item
   */
  getActiveItem: () => activeId.value ? findItemById(activeId.value) : undefined,
  
  /**
   * Expands or collapses a folder
   */
  toggleFolder: (itemId: string) => {
    handleToggle(itemId)
  }
})
</script>

<style scoped>
.tree-view {
  min-height: 100%;
}
</style>