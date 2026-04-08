<template>
  <main class="shell">
    <header class="shell-header">
      <h1>gMall Admin Shell</h1>
      <nav>
        <RouterLink to="/platform">Platform</RouterLink>
        <RouterLink to="/merchant">Merchant</RouterLink>
        <RouterLink to="/supplier">Supplier</RouterLink>
      </nav>
    </header>
    <section class="shell-content">
      <RouterView />
    </section>
  </main>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
  background: linear-gradient(120deg, #f6f7fb, #eef4ff);
  color: #1b2333;
}

.shell-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
}

nav {
  display: flex;
  gap: 16px;
}

.shell-content {
  padding: 24px 32px;
}

a {
  text-decoration: none;
  color: #2a4bd7;
  font-weight: 600;
}
</style>
