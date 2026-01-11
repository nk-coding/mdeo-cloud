<template>
    <Dialog v-model:open="open">
        <DialogContent class="sm:max-w-4xl w-full h-125 flex flex-col">
            <DialogHeader class="sr-only">
                <DialogTitle>Settings</DialogTitle>
            </DialogHeader>

            <Tabs v-model="activeTab" class="flex-1 flex flex-col min-h-0">
                <div class="flex items-center gap-3 pb-3">
                    <div class="flex items-center justify-center h-9 w-9 rounded-full bg-muted/30 text-muted-foreground">
                        <Settings class="w-4 h-4" />
                    </div>
                    <TabsList class="inline-flex h-9 items-center gap-1 rounded-full bg-muted/30 p-1">
                        <TabsTrigger value="users" class="px-4 py-1.5 rounded-full gap-2">
                            <Users class="w-4 h-4" />
                            Users
                        </TabsTrigger>
                        <TabsTrigger value="plugins" class="px-4 py-1.5 rounded-full gap-2">
                            <Puzzle class="w-4 h-4" />
                            Plugins
                        </TabsTrigger>
                    </TabsList>
                </div>

                <TabsContent value="users" class="flex-1 min-h-0">
                    <UserManagement />
                </TabsContent>

                <TabsContent value="plugins" class="flex-1 min-h-0 overflow-hidden">
                    <PluginManagement :backend-api="backendApi" />
                </TabsContent>
            </Tabs>
        </DialogContent>
    </Dialog>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { Users, Puzzle, Settings } from "lucide-vue-next";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import UserManagement from "./UserManagement.vue";
import PluginManagement from "./PluginManagement.vue";
import type { BackendApi } from "@/data/api/backendApi";

const props = defineProps<{
    backendApi: BackendApi;
}>();

const open = defineModel<boolean>("open", { default: false });
const activeTab = ref("users");
</script>
