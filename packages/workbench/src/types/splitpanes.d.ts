declare module 'splitpanes' {
  import { DefineComponent } from 'vue'
  
  export const Splitpanes: DefineComponent<{
    class?: string
    style?: string
    horizontal?: boolean
    pushOtherPanes?: boolean
    dblClickSplitter?: boolean
    resizerStyle?: Record<string, string>
    paneStyle?: Record<string, string>
  }>
  
  export const Pane: DefineComponent<{
    size?: number | string
    minSize?: number | string
    maxSize?: number | string
  }>
}