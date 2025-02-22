package slimeknights.tconstruct.library.book.content;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.element.ImageData;
import slimeknights.mantle.client.book.data.element.TextData;
import slimeknights.mantle.client.screen.book.ArrowButton;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.mantle.client.screen.book.element.BookElement;
import slimeknights.mantle.client.screen.book.element.ImageElement;
import slimeknights.mantle.client.screen.book.element.TextElement;
import slimeknights.mantle.recipe.RecipeHelper;
import slimeknights.mantle.util.ItemStackList;
import slimeknights.tconstruct.library.TinkerRegistries;
import slimeknights.tconstruct.library.Util;
import slimeknights.tconstruct.library.book.TinkerPage;
import slimeknights.tconstruct.library.book.elements.TinkerItemElement;
import slimeknights.tconstruct.library.client.screen.book.element.CycleRecipeElement;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.recipe.RecipeTypes;
import slimeknights.tconstruct.library.recipe.tinkerstation.modifier.AbstractModifierRecipe;
import slimeknights.tconstruct.tools.modifiers.EmptyModifier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ContentModifier extends TinkerPage {

  public static final transient String ID = "modifier";

  public static final transient int TEX_SIZE = 256;

  public static final ResourceLocation BOOK_MODIFY = Util.getResource("textures/gui/book/modify.png");

  public static final transient ImageData IMG_SLOT_1 = new ImageData(BOOK_MODIFY, 0, 75, 22, 22, TEX_SIZE, TEX_SIZE);
  public static final transient ImageData IMG_SLOT_2 = new ImageData(BOOK_MODIFY, 0, 97, 40, 22, TEX_SIZE, TEX_SIZE);
  public static final transient ImageData IMG_SLOT_3 = new ImageData(BOOK_MODIFY, 0, 119, 58, 22, TEX_SIZE, TEX_SIZE);
  public static final transient ImageData IMG_SLOT_4 = new ImageData(BOOK_MODIFY, 0, 141, 58, 41, TEX_SIZE, TEX_SIZE);
  public static final transient ImageData IMG_SLOT_5 = new ImageData(BOOK_MODIFY, 0, 141, 58, 41, TEX_SIZE, TEX_SIZE);

  public static final transient ImageData IMG_TABLE = new ImageData(BOOK_MODIFY, 214, 0, 42, 46, TEX_SIZE, TEX_SIZE);

  public static final transient int[] slotX = new int[]{3, 21, 39, 12, 30};
  public static final transient int[] slotY = new int[]{3, 3, 3, 22, 22};

  private transient Modifier modifier;
  private transient List<AbstractModifierRecipe> recipes;

  private transient int currentRecipe = 0;
  private final transient List<BookElement> parts = new ArrayList<>();

  private final transient Map<AbstractModifierRecipe, List<List<ItemStack>>> modifierRecipeListMap = new HashMap<>();

  public TextData[] text;
  public String[] effects;

  @SerializedName("modifier_id")
  public String modifierID;

  @Override
  public void load() {
    if (this.modifierID == null) {
      this.modifierID = this.parent.name;
    }

    if (this.modifier == null) {
      this.modifier = TinkerRegistries.MODIFIERS.getValue(new ModifierId(this.modifierID));
    }

    if (this.recipes == null) {
      assert Minecraft.getInstance().world != null;
      this.recipes = RecipeHelper.getRecipes(Minecraft.getInstance().world.getRecipeManager(), RecipeTypes.TINKER_STATION, AbstractModifierRecipe.class).stream().filter(abstractModifierRecipe -> abstractModifierRecipe.getDisplayResult().getModifier() == this.modifier).collect(Collectors.toList());
    }
  }

  @Override
  public void build(BookData book, ArrayList<BookElement> list, boolean brightSide) {
    if (this.modifier == null || this.modifier instanceof EmptyModifier || this.recipes.isEmpty()) {
      list.add(new ImageElement(0, 0, 32, 32, ImageData.MISSING));
      System.out.println("Modifier with id " + modifierID + " not found");
      return;
    }

    for (AbstractModifierRecipe recipe : this.recipes) {
      List<List<ItemStack>> inputs = recipe.getDisplayItems();

      this.modifierRecipeListMap.put(recipe, inputs);
    }

    this.addTitle(list, this.modifier.getDisplayName().getString(), true, this.modifier.getColor());

    // description
    int h = BookScreen.PAGE_WIDTH / 3 - 10;
    list.add(new TextElement(10, 20, BookScreen.PAGE_WIDTH - 20, h, text));

    if (this.effects.length > 0) {
      TextData head = new TextData(this.parent.translate("modifier.effect"));
      head.underlined = true;

      list.add(new TextElement(10, 20 + h, BookScreen.PAGE_WIDTH / 2 - 5, BookScreen.PAGE_HEIGHT - h - 20, head));

      List<TextData> effectData = Lists.newArrayList();

      for (String e : this.effects) {
        effectData.add(new TextData("\u25CF "));
        effectData.add(new TextData(e));
        effectData.add(new TextData("\n"));
      }

      list.add(new TextElement(10, 30 + h, BookScreen.PAGE_WIDTH / 2 + 5, BookScreen.PAGE_HEIGHT - h - 20, effectData));
    }

    if (recipes.size() > 1) {
      int col = book.appearance.structureButtonColor;
      int colHover = book.appearance.structureButtonColorHovered;
      list.add(new CycleRecipeElement(BookScreen.PAGE_WIDTH - ArrowButton.ArrowType.RIGHT.w - 32, 160,
        ArrowButton.ArrowType.RIGHT, col, colHover, this, book, list));
    }

    this.buildAndAddRecipeDisplay(book, list, this.modifierRecipeListMap.get(this.recipes.get(this.currentRecipe)), null);
  }

  /**
   * Builds the recipe display to show in the book's gui and adds it so that the user can see the recipes.
   *
   * @param book   the book data
   * @param list   the list of book elements
   * @param inputs the itemstacks to use for the recipe
   * @param parent the parent book screen, only used when there is multiple recipes
   */
  public void buildAndAddRecipeDisplay(BookData book, ArrayList<BookElement> list, @Nullable List<List<ItemStack>> inputs, @Nullable BookScreen parent) {
    if (inputs != null) {
      ImageData img = IMG_SLOT_1;

      if (inputs.size() == 4)
        img = IMG_SLOT_2;
      else if (inputs.size() == 5)
        img = IMG_SLOT_3;
      else if (inputs.size() == 6)
        img = IMG_SLOT_4;
      else if (inputs.size() == 7)
        img = IMG_SLOT_5;

      int imgX = BookScreen.PAGE_WIDTH / 2 + 20;
      int imgY = BookScreen.PAGE_HEIGHT / 2 + 30;

      imgX = imgX + 29 - img.width / 2;
      imgY = imgY + 20 - img.height / 2;

      ImageElement table = new ImageElement(imgX + (img.width - IMG_TABLE.width) / 2, imgY - 24, -1, -1, IMG_TABLE);

      if (parent != null)
        table.parent = parent;

      this.parts.add(table);
      list.add(table); // TODO ADD TABLE TO TEXTURE?

      ImageElement slot = new ImageElement(imgX, imgY, -1, -1, img, book.appearance.slotColor);

      if (parent != null)
        slot.parent = parent;

      this.parts.add(slot);
      list.add(slot);

      ItemStackList demo = getDemoTools(inputs.get(0));

      TinkerItemElement demoTools = new TinkerItemElement(imgX + (img.width - 16) / 2, imgY - 24, 1f, demo);

      if (parent != null)
        demoTools.parent = parent;

      this.parts.add(demoTools);
      list.add(demoTools);

      ImageElement image = new ImageElement(imgX + (img.width - 22) / 2, imgY - 27, -1, -1, IMG_SLOT_1, 0xffffff);

      if (parent != null)
        image.parent = parent;

      this.parts.add(image);
      list.add(image);

      for (int i = 2; i < inputs.size(); i++) {
        TinkerItemElement part = new TinkerItemElement(imgX + slotX[i - 2], imgY + slotY[i - 2], 1f, inputs.get(i));

        if (parent != null)
          part.parent = parent;

        this.parts.add(part);
        list.add(part);
      }
    }
  }

  /**
   * Creates an ItemStackList from a list of itemstacks
   * Used for rendering the tools in the book's modifier screen
   *
   * @param stacks The items to use.
   * @return A itemStackList containing the tools to show with the modifier applied
   */
  protected ItemStackList getDemoTools(List<ItemStack> stacks) {
    ItemStackList demo = ItemStackList.withSize(stacks.size());

    for (int i = 0; i < stacks.size(); i++) {
      demo.set(i, stacks.get(i));
    }

    return demo;
  }

  /**
   * Cycles to the next recipe by deleting the old elements and adding them again.
   *
   * @param book The book data
   * @param list The list of book elements
   */
  public void nextRecipe(BookData book, ArrayList<BookElement> list) {
    this.currentRecipe++;

    if (this.currentRecipe >= this.recipes.size()) {
      this.currentRecipe = 0;
    }

    BookScreen parent = this.parts.get(0).parent;

    for (BookElement element : this.parts) {
      list.remove(element);
    }

    this.parts.clear();

    this.buildAndAddRecipeDisplay(book, list, this.modifierRecipeListMap.get(this.recipes.get(this.currentRecipe)), parent);
  }
}
