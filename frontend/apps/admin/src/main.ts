import { createApp } from "vue";
import { createRouter, createWebHistory } from "vue-router";
import App from "./App.vue";
import PlatformDashboard from "./pages/PlatformDashboard.vue";
import MerchantDashboard from "./pages/MerchantDashboard.vue";
import SupplierDashboard from "./pages/SupplierDashboard.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/platform" },
    { path: "/platform", component: PlatformDashboard },
    { path: "/merchant", component: MerchantDashboard },
    { path: "/supplier", component: SupplierDashboard }
  ]
});

createApp(App).use(router).mount("#app");
