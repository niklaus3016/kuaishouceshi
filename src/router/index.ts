import { createRouter, createWebHistory } from 'vue-router';
import Login from '../pages/Login.vue';
import Home from '../pages/Home.vue';
import Lottery from '../pages/Lottery.vue';
import LotteryDetail from '../pages/LotteryDetail.vue';
import PhoneVerification from '../pages/PhoneVerification.vue';
import WelfareLottery from '../pages/WelfareLottery.vue';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
  },
  { path: '/', name: 'Home', component: Home, beforeEnter: (to, from, next) => { const empId = localStorage.getItem('empId'); if (!empId) { next('/login'); } else { next(); } }, },
  { path: '/lottery', name: 'Lottery', component: Lottery, beforeEnter: (to, from, next) => { const empId = localStorage.getItem('empId'); if (!empId) { next('/login'); } else { next(); } }, },
  { path: '/lottery-detail', name: 'LotteryDetail', component: LotteryDetail, beforeEnter: (to, from, next) => { const empId = localStorage.getItem('empId'); if (!empId) { next('/login'); } else { next(); } }, },
  { path: '/phone-verification', name: 'PhoneVerification', component: PhoneVerification, beforeEnter: (to, from, next) => { const empId = localStorage.getItem('empId'); if (!empId) { next('/login'); } else { next(); } }, },
  { path: '/welfare-lottery', name: 'WelfareLottery', component: WelfareLottery, beforeEnter: (to, from, next) => { const empId = localStorage.getItem('empId'); if (!empId) { next('/login'); } else { next(); } }, },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
