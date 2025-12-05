/**
 * Represents a file system entry that can be either a file or folder
 */
export interface FileSystemEntry {
  /** Unique identifier for the entry */
  id: string
  /** Display name of the entry */
  name: string
  /** Type of the entry - file or folder */
  type: 'file' | 'folder'
  /** Content for files, undefined for folders */
  content?: string
  /** Child entries for folders */
  children?: FileSystemEntry[]
  /** Parent folder id, undefined for root entries */
  parentId?: string
}

/**
 * In-memory browser file system implementation
 */
export class BrowserFileSystem {
  private entries = new Map<string, FileSystemEntry>()
  private rootEntries = new Set<string>()
  private nextId = 1

  constructor() {
    this.setupDefaultStructure()
  }

  /**
   * Sets up the default folder structure with sample files
   */
  private setupDefaultStructure(): void {
    const folder1 = this.createFolder('models', undefined)
    const folder2 = this.createFolder('components', undefined)
    
    const folder1Sub1 = this.createFolder('entities', folder1.id)
    const folder1Sub2 = this.createFolder('relations', folder1.id)
    
    const folder2Sub1 = this.createFolder('ui', folder2.id)
    const folder2Sub2 = this.createFolder('forms', folder2.id)

    // Add sample files
    this.createFile('user.metamodel', 'class User {\n  name: string\n  email: string\n}', folder1Sub1.id)
    this.createFile('product.metamodel', 'class Product {\n  title: string\n  price: int\n}', folder1Sub1.id)
    
    this.createFile('associations.metamodel', 'User.orders[*] -- Order.customer', folder1Sub2.id)
    this.createFile('inheritance.metamodel', 'class Admin extends User {\n  role: string\n}', folder1Sub2.id)
    
    this.createFile('button.metamodel', 'class Button {\n  label: string\n  onClick: function\n}', folder2Sub1.id)
    this.createFile('input.metamodel', 'class Input {\n  placeholder: string\n  value: string\n}', folder2Sub1.id)
    
    this.createFile('form.metamodel', 'class Form {\n  fields[*]: FormField\n  submit: function\n}', folder2Sub2.id)
    this.createFile('validation.metamodel', 'class Validator {\n  rules: string[*]\n  validate: function\n}', folder2Sub2.id)
  }

  /**
   * Generates a unique ID for new entries
   */
  private generateId(): string {
    return `entry_${this.nextId++}`
  }

  /**
   * Creates a new folder in the file system
   */
  createFolder(name: string, parentId?: string): FileSystemEntry {
    const entry: FileSystemEntry = {
      id: this.generateId(),
      name,
      type: 'folder',
      children: [],
      parentId
    }

    this.entries.set(entry.id, entry)
    
    if (parentId) {
      const parent = this.entries.get(parentId)
      if (parent && parent.type === 'folder') {
        parent.children!.push(entry)
      }
    } else {
      this.rootEntries.add(entry.id)
    }

    return entry
  }

  /**
   * Creates a new file in the file system
   */
  createFile(name: string, content: string, parentId?: string): FileSystemEntry {
    const entry: FileSystemEntry = {
      id: this.generateId(),
      name,
      type: 'file',
      content,
      parentId
    }

    this.entries.set(entry.id, entry)
    
    if (parentId) {
      const parent = this.entries.get(parentId)
      if (parent && parent.type === 'folder') {
        parent.children!.push(entry)
      }
    } else {
      this.rootEntries.add(entry.id)
    }

    return entry
  }

  /**
   * Gets an entry by its ID
   */
  getEntry(id: string): FileSystemEntry | undefined {
    return this.entries.get(id)
  }

  /**
   * Gets all root entries
   */
  getRootEntries(): FileSystemEntry[] {
    return Array.from(this.rootEntries).map(id => this.entries.get(id)!).filter(Boolean)
  }

  /**
   * Renames an entry
   */
  renameEntry(id: string, newName: string): boolean {
    const entry = this.entries.get(id)
    if (!entry) return false
    
    entry.name = newName
    return true
  }

  /**
   * Deletes an entry and all its children
   */
  deleteEntry(id: string): boolean {
    const entry = this.entries.get(id)
    if (!entry) return false

    // Remove from parent's children
    if (entry.parentId) {
      const parent = this.entries.get(entry.parentId)
      if (parent && parent.type === 'folder') {
        parent.children = parent.children!.filter(child => child.id !== id)
      }
    } else {
      this.rootEntries.delete(id)
    }

    // Recursively delete children for folders
    if (entry.type === 'folder' && entry.children) {
      for (const child of entry.children) {
        this.deleteEntry(child.id)
      }
    }

    this.entries.delete(id)
    return true
  }

  /**
   * Gets the file extension from a filename
   */
  getFileExtension(filename: string): string {
    const lastDot = filename.lastIndexOf('.')
    return lastDot > 0 ? filename.substring(lastDot) : ''
  }

  /**
   * Gets the filename without extension
   */
  getBaseName(filename: string): string {
    const lastDot = filename.lastIndexOf('.')
    return lastDot > 0 ? filename.substring(0, lastDot) : filename
  }
}