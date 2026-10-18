import java.lang.reflect.Field;
public class TestAttr {
    public static void main(String[] a) throws Exception {
        Class<?> c = Class.forName("net.minecraft.world.entity.LivingEntity", false, TestAttr.class.getClassLoader());
        Field f = c.getDeclaredField("f_20961_");
        f.setAccessible(true);
        System.out.println("field OK: " + f);
    }
}
