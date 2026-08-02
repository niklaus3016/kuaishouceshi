import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function cleanAllLoginRecords() {
  try {
    console.log('开始清理用户2222的所有登录记录...');

    // 删除用户2222的所有登录记录
    const result = await prisma.userLogin.deleteMany({
      where: {
        userId: 'user_2222_1773112309254',
        employeeId: '2222'
      }
    });

    console.log(`已删除 ${result.count} 条登录记录`);

    // 查询剩余的登录记录
    const remainingRecords = await prisma.userLogin.findMany({
      where: {
        userId: 'user_2222_1773112309254',
        employeeId: '2222'
      }
    });

    console.log('剩余登录记录:', remainingRecords);

  } catch (error) {
    console.error('清理登录记录失败:', error);
  } finally {
    await prisma.$disconnect();
  }
}

cleanAllLoginRecords();