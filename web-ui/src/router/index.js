import { createRouter, createWebHistory } from 'vue-router'
import AboutView from '../views/AboutView.vue'
import ArticlesView from '../views/ArticlesView.vue'
import HomeView from '../views/HomeView.vue'
import ProjectsView from '../views/ProjectsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/articles', name: 'articles', component: ArticlesView },
    { path: '/projects', name: 'projects', component: ProjectsView },
    { path: '/about', name: 'about', component: AboutView },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

export default router
