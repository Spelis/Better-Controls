package chylex.bettercontrols.gui.elements;
import chylex.bettercontrols.gui.OptionListWidget.Widget;
import chylex.bettercontrols.util.LiteralText;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IReorderingProcessor;
import java.util.List;
import static chylex.bettercontrols.util.Statics.MINECRAFT;

public final class TextWidget extends AbstractGui implements Widget{
	public static final int LEFT = 0;
	public static final int CENTER = 1;
	
	private final LiteralText text;
	private int x;
	private int y;
	private final int width;
	private final int height;
	private final int align;
	
	public TextWidget(final int x, final int y, final int width, final int height, final LiteralText text, final int align){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.text = text;
		this.align = align;
	}
	
	public TextWidget(final int x, final int y, final int width, final LiteralText text, final int align){
		this(x, y, width, 20, text, align);
	}
	
	public TextWidget(final int x, final int y, final int width, final LiteralText text){
		this(x, y, width, 20, text, LEFT);
	}
	
	@Override
	public int getX(){
		return x;
	}
	
	@Override
	public int getY(){
		return y;
	}
	
	@Override
	public void setX(final int x){
		this.x = x;
	}
	
	@Override
	public void setY(final int y){
		this.y = y;
	}
	
	@Override
	public void render(final MatrixStack matrices, final int mouseX, final int mouseY, final float delta){
		final FontRenderer textRenderer = MINECRAFT.fontRenderer;
		final List<IReorderingProcessor> lines = textRenderer.trimStringToWidth(text, width);
		final int lineHeight = textRenderer.FONT_HEIGHT + 1;
		
		final int finalX = align == CENTER ? x + (width / 2) - (lines.stream().mapToInt(textRenderer::func_243245_a).max().orElse(0) / 2) : x;
		final int finalY = y + (height / 2) - (lineHeight * lines.size() / 2) + 1;
		
		for(int i = 0; i < lines.size(); i++){
			final IReorderingProcessor line = lines.get(i);
			textRenderer.func_238407_a_(matrices, line, finalX, finalY + (i * lineHeight), (255 << 16) | (255 << 8) | 255);
		}
	}
}
