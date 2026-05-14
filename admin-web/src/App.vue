<template>
  <router-view />
</template>

<script setup>
// 根组件：仅承载路由视图与全局样式。
</script>

<style>
/* 全局基础样式：统一盒模型与字体。 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "微软雅黑", Arial, sans-serif;
}
</style>