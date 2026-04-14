<template>
  <div class="dashboard">
    <h2 class="page-title">
      <Home theme="outline" size="30" fill="#5c4b3a" />
      <span>首页</span>
    </h2>
    <div class="dashboard-cards">
      <div class="card">
        <div class="card-icon">
          <EveryUser theme="outline" size="30" fill="#8b7355" />
        </div>
        <div class="card-content">
          <h3 class="card-title">用户数量</h3>
          <p class="card-value">100+</p>
        </div>
      </div>
      <div class="card">
        <div class="card-icon">
          <Peoples theme="outline" size="30" fill="#8b7355" />
        </div>
        <div class="card-content">
          <h3 class="card-title">角色数量</h3>
          <p class="card-value">10+</p>
        </div>
      </div>
      <div class="card">
        <div class="card-icon">
          <Lock theme="outline" size="30" fill="#8b7355" />
        </div>
        <div class="card-content">
          <h3 class="card-title">权限数量</h3>
          <p class="card-value">50+</p>
        </div>
      </div>
      <div class="card">
        <div class="card-icon">
          <MessageOne theme="outline" size="30" fill="#8b7355" />
        </div>
        <div class="card-content">
          <h3 class="card-title">对话次数</h3>
          <p class="card-value">1000+</p>
        </div>
      </div>
    </div>
    <div class="dashboard-info">
      <h3 class="info-title">系统信息</h3>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">系统版本</span>
          <span class="info-value">v1.0.0</span>
        </div>
        <div class="info-item">
          <span class="info-label">最后更新</span>
          <span class="info-value">2026-04-08</span>
        </div>
        <div class="info-item">
          <span class="info-label">服务器状态</span>
          <span class="info-value status-online">在线</span>
        </div>
        <div class="info-item">
          <span class="info-label">API状态</span>
          <span class="info-value status-online">正常</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Home, EveryUser, Peoples, Lock, MessageOne } from '@icon-park/vue-next'
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 1100px;
  margin: 0 auto;
  width: 100%;
}

.page-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 600;
  color: #3d3d3d;
  margin: 0;
  text-align: center;
}

.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.card {
  background-color: #faf6f0;
  border: 1px solid #e0d5c5;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s;
}

.card:hover {
  border-color: #d4a574;
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(139, 115, 85, 0.12);
}

.card-icon {
  font-size: 32px;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ede6d8;
  border-radius: 4px;
}

.card-content {
  flex: 1;
}

.card-title {
  font-size: 14px;
  color: #6b6b6b;
  margin: 0 0 8px;
  font-weight: 500;
}

.card-value {
  font-size: 24px;
  font-weight: 600;
  color: #8b7355;
  margin: 0;
}

.dashboard-info {
  background-color: #faf6f0;
  border: 1px solid #e0d5c5;
  border-radius: 10px;
  padding: 24px;
}

.info-title {
  font-size: 18px;
  font-weight: 600;
  color: #3d3d3d;
  margin: 0 0 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 14px;
  color: #6b6b6b;
}

.info-value {
  font-size: 16px;
  font-weight: 500;
  color: #3d3d3d;
}

.status-online {
  color: #7a9e6e;
}

.status-offline {
  color: #c17b7b;
}
</style>