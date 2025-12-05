<template>
  <div class="file-system-explorer h-full">
    <ContextMenu>
      <ContextMenuTrigger class="h-full">
        <TreeView
          ref="treeView"
          :items="rootEntries"
          @activate="handleItemActivate"
          @rename="handleItemRename"
        />
      </ContextMenuTrigger>
      
      <ContextMenuContent>
        <ContextMenuItem
          v-if="contextItem?.type === 'folder'"
          @select="handleCreateFile"
        >
          <File class="mr-2 h-4 w-4" />
          Create Metamodel File
        </ContextMenuItem>
        
        <ContextMenuItem
          v-if="contextItem?.type === 'folder'"
          @select="handleCreateFolder"
        >
          <Folder class="mr-2 h-4 w-4" />
          Create Folder
        </ContextMenuItem>
        
        <ContextMenuItem
          v-if="!contextItem"
          @select="handleCreateRootFolder"
        >
          <Folder class="mr-2 h-4 w-4" />
          Create Folder
        </ContextMenuItem>
        
        <ContextMenuSeparator v-if="contextItem" />
        
        <ContextMenuItem
          v-if="contextItem"
          @select="handleRename"
        >
          <Edit class="mr-2 h-4 w-4" />
          Rename
        </ContextMenuItem>
        
        <ContextMenuItem
          v-if="contextItem"
          @select="handleDelete"
          class="text-destructive focus:text-destructive"
        >
          <Trash2 class="mr-2 h-4 w-4" />
          Delete
        </ContextMenuItem>
      </ContextMenuContent>
    </ContextMenu>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, useTemplateRef } from 'vue'
import { File, Folder, Edit, Trash2 } from 'lucide-vue-next'
import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuSeparator,
  ContextMenuTrigger,
} from '@/components/ui/context-menu'
import TreeView from './TreeView.vue'
import type { BrowserFileSystem, FileSystemEntry } from '@/lib/filesystem'

/**
 * Props for the FileSystemExplorer component
 */
interface FileSystemExplorerProps {
  /** The file system instance to explore */
  fileSystem: BrowserFileSystem
}

/**
 * Events emitted by the FileSystemExplorer component
 */
interface FileSystemExplorerEmits {
  /** Emitted when a file is activated/opened */
  'file-activate': [entry: FileSystemEntry]
  /** Emitted when a folder is activated */
  'folder-activate': [entry: FileSystemEntry]
  /** Emitted when the file system changes */
  'fs-change': []
}

const props = defineProps<FileSystemExplorerProps>()
const emit = defineEmits<FileSystemExplorerEmits>()

// Template refs
const treeView = useTemplateRef('treeView')

// Internal state
const contextItem = ref<FileSystemEntry | null>(null)

// Computed properties
const rootEntries = computed(() => props.fileSystem.getRootEntries())

/**
 * Handles item activation in the tree view
 */
function handleItemActivate(item: FileSystemEntry): void {
  contextItem.value = item
  
  if (item.type === 'file') {
    emit('file-activate', item)
  } else {
    emit('folder-activate', item)
  }
}

/**
 * Handles item rename in the tree view
 */
function handleItemRename({ item, newName }: { item: FileSystemEntry; newName: string }): void {
  const success = props.fileSystem.renameEntry(item.id, newName)
  if (success) {
    emit('fs-change')
  }
}

/**
 * Creates a new metamodel file in the context folder
 */
function handleCreateFile(): void {
  if (!contextItem.value || contextItem.value.type !== 'folder') return
  
  let fileName = 'untitled.metamodel'
  let counter = 1
  
  // Find unique name
  while (contextItem.value.children?.some(child => child.name === fileName)) {
    fileName = `untitled${counter}.metamodel`
    counter++
  }
  
  const newFile = props.fileSystem.createFile(
    fileName,
    '// New metamodel file\n',
    contextItem.value.id
  )
  
  emit('fs-change')
  
  // Start editing the new file name
  setTimeout(() => {
    const treeViewInstance = treeView.value
    if (treeViewInstance) {
      treeViewInstance.focusItem(newFile.id)
      // Trigger edit mode - this would need to be implemented in TreeView
    }
  }, 100)
}

/**
 * Creates a new folder in the context folder
 */
function handleCreateFolder(): void {
  if (!contextItem.value || contextItem.value.type !== 'folder') return
  
  let folderName = 'New Folder'
  let counter = 1
  
  // Find unique name
  while (contextItem.value.children?.some(child => child.name === folderName)) {
    folderName = `New Folder ${counter}`
    counter++
  }
  
  const newFolder = props.fileSystem.createFolder(folderName, contextItem.value.id)
  emit('fs-change')
  
  // Start editing the new folder name
  setTimeout(() => {
    const treeViewInstance = treeView.value
    if (treeViewInstance) {
      treeViewInstance.focusItem(newFolder.id)
      // Trigger edit mode - this would need to be implemented in TreeView
    }
  }, 100)
}

/**
 * Creates a new folder at root level
 */
function handleCreateRootFolder(): void {
  let folderName = 'New Folder'
  let counter = 1
  
  // Find unique name at root level
  const rootEntries = props.fileSystem.getRootEntries()
  while (rootEntries.some(entry => entry.name === folderName)) {
    folderName = `New Folder ${counter}`
    counter++
  }
  
  const newFolder = props.fileSystem.createFolder(folderName)
  emit('fs-change')
  
  // Start editing the new folder name
  setTimeout(() => {
    const treeViewInstance = treeView.value
    if (treeViewInstance) {
      treeViewInstance.focusItem(newFolder.id)
      // Trigger edit mode - this would need to be implemented in TreeView
    }
  }, 100)
}

/**
 * Starts renaming the context item
 */
function handleRename(): void {
  if (!contextItem.value) return
  
  const treeViewInstance = treeView.value
  if (treeViewInstance) {
    treeViewInstance.focusItem(contextItem.value.id)
    // Trigger edit mode - this would need to be implemented in TreeView
    setTimeout(() => {
      // This would trigger edit mode on the focused item
      const event = new KeyboardEvent('keydown', { key: 'F2' })
      document.dispatchEvent(event)
    }, 50)
  }
}

/**
 * Deletes the context item
 */
function handleDelete(): void {
  if (!contextItem.value) return
  
  const success = props.fileSystem.deleteEntry(contextItem.value.id)
  if (success) {
    contextItem.value = null
    emit('fs-change')
  }
}
</script>

<style scoped>
.file-system-explorer {
  background: hsl(var(--background));
  border-right: 1px solid hsl(var(--border));
}
</style>