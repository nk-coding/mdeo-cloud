<template>
  <div
    :class="cn(
      'group flex items-center gap-2 px-2 py-1 text-sm cursor-pointer select-none',
      'hover:bg-muted/80 focus:bg-muted/80 focus:outline-none',
      {
        'bg-accent/50 text-accent-foreground': isActive && !isFocused,
        'bg-primary/20 text-primary-foreground': isFocused && !isActive,
        'bg-primary/30 text-primary-foreground': isFocused && isActive,
      }
    )"
    :style="{ paddingLeft: `${depth * 12 + 8}px` }"
    :tabindex="isFocusable ? 0 : -1"
    @click="handleClick"
    @keydown="handleKeyDown"
    @focus="handleFocus"
    @blur="handleBlur"
  >
    <!-- Folder icon or file icon -->
    <component
      :is="iconComponent"
      :class="cn('h-4 w-4 shrink-0', {
        'transform transition-transform': item.type === 'folder',
        'rotate-90': item.type === 'folder' && isExpanded
      })"
    />
    
    <!-- Editable name or display name -->
    <template v-if="isEditing">
      <Input
        v-if="item.type === 'file'"
        ref="editInput"
        v-model="editBaseName"
        class="h-6 text-xs flex-1 min-w-0"
        @keydown.enter="handleSaveEdit"
        @keydown.escape="handleCancelEdit"
        @blur="handleSaveEdit"
      />
      <span v-if="item.type === 'file'" class="text-muted-foreground shrink-0">{{ fileExtension }}</span>
      <Input
        v-if="item.type === 'folder'"
        ref="editInput"
        v-model="editName"
        class="h-6 text-xs flex-1 min-w-0"
        @keydown.enter="handleSaveEdit"
        @keydown.escape="handleCancelEdit"
        @blur="handleSaveEdit"
      />
    </template>
    
    <span v-else class="truncate flex-1 min-w-0">{{ item.name }}</span>
  </div>
  
  <!-- Children (for folders) -->
  <template v-if="item.type === 'folder' && isExpanded && item.children">
    <TreeViewItem
      v-for="child in item.children"
      :key="child.id"
      :item="child"
      :depth="depth + 1"
      :active-id="activeId"
      :focused-id="focusedId"
      :is-focusable="isFocusable"
      @toggle="$emit('toggle', $event)"
      @activate="$emit('activate', $event)"
      @focus-change="$emit('focus-change', $event)"
      @start-edit="$emit('start-edit', $event)"
      @save-edit="$emit('save-edit', $event)"
    />
  </template>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, useTemplateRef, watch, inject } from 'vue'
import { ChevronRight, File, Folder, FolderOpen } from 'lucide-vue-next'
import type { FileSystemEntry } from '@/lib/filesystem'
import { Input } from '@/components/ui/input'
import { cn } from '@/lib/utils'

/**
 * Props for the TreeViewItem component
 */
interface TreeViewItemProps {
  /** The file system entry to display */
  item: FileSystemEntry
  /** Nesting depth for indentation */
  depth?: number
  /** ID of the currently active item */
  activeId?: string
  /** ID of the currently focused item */
  focusedId?: string
  /** Whether this item can receive focus */
  isFocusable?: boolean
}

/**
 * Events emitted by the TreeViewItem component
 */
interface TreeViewItemEmits {
  /** Emitted when a folder is toggled */
  toggle: [id: string]
  /** Emitted when an item is activated */
  activate: [id: string]
  /** Emitted when focus changes */
  'focus-change': [id: string | null]
  /** Emitted when editing starts */
  'start-edit': [id: string]
  /** Emitted when editing is saved */
  'save-edit': [{ id: string; newName: string }]
}

const props = withDefaults(defineProps<TreeViewItemProps>(), {
  depth: 0,
  isFocusable: true
})

const emit = defineEmits<TreeViewItemEmits>()

// Internal state
const editingItems = ref(new Set<string>())
const editName = ref('')
const editBaseName = ref('')
const editInput = useTemplateRef('editInput')

// Inject expansion state checker from parent TreeView
const isItemExpanded = inject<(id: string) => boolean>('isItemExpanded', () => false)

// Computed properties
const isExpanded = computed(() => isItemExpanded(props.item.id))
const isActive = computed(() => props.activeId === props.item.id)
const isFocused = computed(() => props.focusedId === props.item.id)
const isEditing = computed(() => editingItems.value.has(props.item.id))

const fileExtension = computed(() => {
  if (props.item.type !== 'file') return ''
  const lastDot = props.item.name.lastIndexOf('.')
  return lastDot > 0 ? props.item.name.substring(lastDot) : ''
})

const iconComponent = computed(() => {
  if (props.item.type === 'folder') {
    return isExpanded.value ? FolderOpen : Folder
  }
  return File
})

// Watch for external editing requests
watch(() => editingItems.value.has(props.item.id), (isCurrentlyEditing) => {
  if (isCurrentlyEditing) {
    if (props.item.type === 'file') {
      const lastDot = props.item.name.lastIndexOf('.')
      editBaseName.value = lastDot > 0 ? props.item.name.substring(0, lastDot) : props.item.name
    } else {
      editName.value = props.item.name
    }
    
    nextTick(() => {
      const inputEl = editInput.value?.$el as HTMLInputElement
      if (inputEl) {
        inputEl.focus()
        inputEl.select()
      }
    })
  }
})

/**
 * Handles click events on the item
 */
function handleClick(): void {
  if (props.item.type === 'folder') {
    emit('toggle', props.item.id)
    toggleExpanded()
  }
  emit('activate', props.item.id)
}

/**
 * Handles keyboard navigation
 */
function handleKeyDown(event: KeyboardEvent): void {
  switch (event.key) {
    case 'Enter':
    case ' ':
      event.preventDefault()
      if (props.item.type === 'folder') {
        emit('toggle', props.item.id)
        toggleExpanded()
      }
      emit('activate', props.item.id)
      break
    case 'ArrowRight':
      event.preventDefault()
      if (props.item.type === 'folder' && !isExpanded.value) {
        emit('toggle', props.item.id)
        toggleExpanded()
      }
      break
    case 'ArrowLeft':
      event.preventDefault()
      if (props.item.type === 'folder' && isExpanded.value) {
        emit('toggle', props.item.id)
        toggleExpanded()
      }
      break
    case 'F2':
      event.preventDefault()
      startEdit()
      break
  }
}

/**
 * Handles focus events
 */
function handleFocus(): void {
  emit('focus-change', props.item.id)
}

/**
 * Handles blur events
 */
function handleBlur(): void {
  // Only emit focus-change if we're not switching to editing
  if (!isEditing.value) {
    emit('focus-change', null)
  }
}

/**
 * Toggles the expanded state of a folder
 */
function toggleExpanded(): void {
  if (props.item.type === 'folder') {
    emit('toggle', props.item.id)
  }
}

/**
 * Starts editing the item name
 */
function startEdit(): void {
  editingItems.value.add(props.item.id)
  emit('start-edit', props.item.id)
}

/**
 * Saves the edited name
 */
function handleSaveEdit(): void {
  const newName = props.item.type === 'file' 
    ? editBaseName.value + fileExtension.value
    : editName.value
    
  if (newName.trim() && newName !== props.item.name) {
    emit('save-edit', { id: props.item.id, newName: newName.trim() })
  }
  
  editingItems.value.delete(props.item.id)
}

/**
 * Cancels editing
 */
function handleCancelEdit(): void {
  editingItems.value.delete(props.item.id)
}

// Expose methods for external control
defineExpose({
  startEdit,
  isExpanded: () => isExpanded.value,
  toggleExpanded
})
</script>