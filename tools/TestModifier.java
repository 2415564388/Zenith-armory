import net.minecraft.world.entity.ai.attributes.*;
import java.util.UUID;
public class TestModifier {
    public static void main(String[] args) throws Exception {
        RangedAttribute attr = new RangedAttribute("test", 0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        AttributeInstance inst = new AttributeInstance(attr, x -> {});
        inst.m_22100_(10.0);
        UUID id = UUID.fromString("d4f0c9a2-3b1e-4c5a-9e7f-1a2b3c4d5e6f");
        inst.m_22118_(new AttributeModifier(id, "pct", 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL));
        System.out.println("base 10, +10% -> " + inst.m_22135_());
        inst.m_22120_(id);
        System.out.println("removed -> " + inst.m_22135_());
    }
}
