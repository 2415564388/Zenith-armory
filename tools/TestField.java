import java.lang.reflect.Field;
public class TestField {
    public static void main(String[] args) throws Exception {
        Class<?> c = Class.forName("com.tacz.guns.entity.EntityKineticBullet", false, TestField.class.getClassLoader());
        Field f = c.getDeclaredField("damageModifier");
        f.setAccessible(true);
        System.out.println("field OK: " + f);
    }
}
