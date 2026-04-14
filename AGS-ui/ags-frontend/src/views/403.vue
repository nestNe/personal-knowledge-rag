<template>
  <div class="error-page">
    <div class="error-container">
      <h1 class="error-code">403</h1>
      <h2 class="error-title">无权限访问</h2>
      <p class="error-message">抱歉，您没有权限访问该页面</p>
      <router-link to="/" class="error-btn">返回首页</router-link>
    </div>
  </div>
</template>

<script setup>
// 403页面
</script>

<style scoped>
.error-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f0e6;
  color: #3d3d3d;
}

.error-container {
  text-align: center;
  padding: 40px;
  background-color: #faf6f0;
  border: 1px solid #e0d5c5;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(139, 115, 85, 0.14);
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: #c17b7b;
  margin: 0 0 20px;
}

.error-title {
  font-size: 32px;
  font-weight: 600;
  margin: 0 0 16px;
  color: #3d3d3d;
}

.error-message {
  font-size: 16px;
  color: #6b6b6b;
  margin: 0 0 32px;
}

.error-btn {
  display: inline-block;
  padding: 12px 24px;
  background-color: #d4a574;
  color: #3d3d3d;
  border: 1px solid #c99564;
  text-decoration: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.error-btn:hover {
  background-color: #c99564;
}
</style>