package gregtechmod.api.recipe;

import gregtechmod.api.GregTech_API;
import gregtechmod.api.enums.GT_Items;
import gregtechmod.api.enums.Materials;
import gregtechmod.api.interfaces.IGregTechTileEntity;
import gregtechmod.api.util.GT_Log;
import gregtechmod.api.util.GT_ModHandler;
import gregtechmod.api.util.GT_OreDictUnificator;
import gregtechmod.api.util.GT_Utility;
import gregtechmod.api.util.ItemStackKey;

import static gregtechmod.api.recipe.RecipeMaps.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * NEVER INCLUDE THIS FILE IN YOUR MOD!!!
 * 
 * This File contains the functions used for Recipes. Please do not include this File AT ALL in your Moddownload as it ruins compatibility
 * This is just the Core of my Recipe System, if you just want to GET the Recipes I add, then you can access this File.
 * Do NOT add Recipes using the Constructors inside this Class, The GregTech_API File calls the correct Functions for these Constructors.
 * 
 * I know this File causes some Errors, because of missing Main Functions, but if you just need to compile Stuff, then remove said erroreous Functions.
 */
public class Recipe {
	public static volatile int VERSION = 408;
	
	/** It is an IdentityHashMap, because it uses a List as Key, and since that List changes (and therefore the Result of the equals Method), the Key is not secure, while the Identity is. */
	private static final IdentityHashMap<List<Recipe>, HashMap<Integer, List<Recipe>>> sRecipeMappings = new IdentityHashMap<List<Recipe>, HashMap<Integer, List<Recipe>>>();
	
	public static void reInit() {
//        GT_Log.log.info("GT_Mod: Re-Unificating Recipes.");
//        for (Entry<List<GT_Recipe>, HashMap<Integer, List<GT_Recipe>>> tMapEntry : sRecipeMappings.entrySet()) {
//        	HashMap<Integer, List<GT_Recipe>> tMap = tMapEntry.getValue();
//        	if (tMap != null) tMap.clear();
//        	for (GT_Recipe tRecipe : tMapEntry.getKey()) {
//            	GT_OreDictUnificator.setStackArray(true, tRecipe.mInputs);
//            	GT_OreDictUnificator.setStackArray(true, tRecipe.mOutputs);
//            	if (tMap != null) tRecipe.addToMap(tMap);
//        	}
//        }
	}
	
	public ItemStack[][] mInputs;
	public ItemStack[] mOutputs;
	public int mDuration;
	public int mEUt;
	public int mStartEU;
	// Use this to just disable a specific Recipe, but the Config enables that already for every single Recipe.
	public boolean mEnabled = true;
	
	public ItemStack[][] getRepresentativeInputs() {
		ItemStack[][] copy = new ItemStack[mInputs.length][];
		for (int i = 0; i < mInputs.length; i++) {
			ItemStack[] copy1 = new ItemStack[mInputs[i].length];
			for (int j = 0; j < mInputs[i].length; j++) {
				copy1[j] = mInputs[i][j].copy();
			}
			copy[i] = copy1;
		}
		
		return copy;
	}
	
	public ItemStack[] getOutputs() {
		return Arrays.stream(mOutputs)
				.filter(e -> e != null)
				.map(stack -> stack.copy())
				.toArray(i -> new ItemStack[i]);
	}
	
	public boolean match(ItemStack...machineInputs) {
		assert machineInputs != null : "Recipe check failed, machineInputs = null";
		assert machineInputs.length > 0 : "Recipe check failed, machineInputs size < 1";
		 
		if (machineInputs.length >= mInputs.length) {
			start:
			for (ItemStack input : Arrays.stream(machineInputs).filter(s -> GT_Utility.isStackValid(s)).collect(Collectors.toList())) {
				for (ItemStack[] slot : mInputs) {
					List<ItemStackKey> variants = Arrays.stream(slot)
							.map(s -> ItemStackKey.from(s))
							.collect(Collectors.toList());
					if (variants.contains(ItemStackKey.from(input))) {
						continue start;
					}
				}
				
				return false;
			}
		} else return false;
		
		return true;
	}
	
	public boolean match(boolean decrease, IGregTechTileEntity tile, int[] inputSlots) {
		assert tile != null : "Recipe check failed, tile = null";
		assert inputSlots != null : "Recipe check failed, inputSlots = null";
		assert inputSlots.length > 0 : "Recipe check failed, inputSlots size < 1";
		
		Map<Integer, ItemStack> decreaseMap = new HashMap<>();
		Map<Integer, ItemStack> slotStacks = new HashMap<>();
		for (int i : inputSlots) {
			ItemStack stack = tile.getStackInSlot(i);
			if (stack != null)
				slotStacks.put(i, stack);
		}
		
		for (ItemStack[] slot : mInputs) {
			Map<ItemStackKey, Integer> variants = Arrays.stream(slot)
					.collect(Collectors.toMap(stack -> ItemStackKey.from(stack), stack -> stack.stackSize));
			Iterator<Entry<Integer, ItemStack>> iter = slotStacks.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<Integer, ItemStack> value = iter.next();
				ItemStackKey temp = ItemStackKey.from(value.getValue());
				if ((variants.keySet().contains(temp))) {
					ItemStack recipeItem = temp.get();
					recipeItem.stackSize = variants.get(temp);
					ItemStack slotItem = value.getValue();
					if (recipeItem.stackSize <= slotItem.stackSize) {
						decreaseMap.put(value.getKey(), recipeItem);
						iter.remove();
						break;
					} else return false;
				}
				return false;
			}
		}
		
		if (decrease && decreaseMap.size() == mInputs.length) for (Entry<Integer, ItemStack> e : decreaseMap.entrySet()) {
			tile.decrStackSize(e.getKey(), e.getValue().stackSize);
		}
		
		return decreaseMap.size() == mInputs.length;
	}
	
	private final void addToMap(HashMap<Integer, List<Recipe>> aMap) {
		for (ItemStack[] tStacks : mInputs) {
			for (ItemStack tStack : tStacks) if (tStack != null) {
				Integer tIntStack = GT_Utility.stackToInt(tStack);
				List<Recipe> tList = aMap.get(tIntStack);
				if (tList == null) aMap.put(tIntStack, tList = new ArrayList<Recipe>(2));
				tList.add(this);
			}
		}
	}
	
	private final void addToLists(List<Recipe> aList) {
		HashMap<Integer, List<Recipe>> aMap = sRecipeMappings.get(aList);
		if (aMap == null) sRecipeMappings.put(aList, aMap = new HashMap<Integer, List<Recipe>>());
		aList.add(this);
		addToMap(aMap);
	}
	
	public static Recipe findEqualRecipe(boolean aShapeless, boolean aNotUnificated, List<Recipe> aList, ItemStack...aInputs) {
		if (aInputs.length < 1) return null;
		HashMap<Integer, List<Recipe>> tMap = sRecipeMappings.get(aList);
		if (aNotUnificated) GT_OreDictUnificator.setStackArray(true, aInputs);
		if (tMap == null) {
			for (Recipe tRecipe : aList) if (tRecipe.match(aInputs)) return tRecipe.mEnabled?tRecipe:null;
		} else {
			for (ItemStack tStack : aInputs) if (tStack != null) {
				aList = tMap.get(GT_Utility.stackToInt(tStack));
				if (aList != null) for (Recipe tRecipe : aList) if (tRecipe.match(aInputs)) return tRecipe.mEnabled?tRecipe:null;
				aList = tMap.get(GT_Utility.stackToWildcard(tStack));
				if (aList != null) for (Recipe tRecipe : aList) if (tRecipe.match(aInputs)) return tRecipe.mEnabled?tRecipe:null;
			}
		}
		return null;
	}
	
	public void checkCellBalance() {
		if (!GregTech_API.SECONDARY_DEBUG_MODE || mInputs.length < 1) return;
		
		int tInputAmount  = GT_ModHandler.getCapsuleCellContainerCountMultipliedWithStackSize(getFirstInputs());
		int tOutputAmount = GT_ModHandler.getCapsuleCellContainerCountMultipliedWithStackSize(mOutputs);
		
		if (tInputAmount < tOutputAmount) {
			if (!Materials.Tin.contains(getFirstInputs())) {
				GT_Log.log.catching(new Exception());
			}
		} else if (tInputAmount > tOutputAmount) {
			if (!Materials.Tin.contains(mOutputs)) {
				GT_Log.log.catching(new Exception());
			}
		}
	}
	
	public ItemStack[] getFirstInputs() {
		ItemStack[] res = new ItemStack[mInputs.length];
		for (int i = 0; i < res.length; i++) 
			res[i] = mInputs[i][0];
		return res;
	}
	
	public static boolean addRecipe(List<Recipe> aList, boolean aShapeless, ItemStack aInput1, ItemStack aInput2, ItemStack aOutput1, ItemStack aOutput2, ItemStack aOutput3, ItemStack aOutput4, int aDuration, int aEUt, int aStartEU) {
		return addRecipe(aList, aShapeless, new Recipe(aInput1, aInput2, aOutput1, aOutput2, aOutput3, aOutput4, aDuration, aEUt, aStartEU));
	}
	
	public static boolean addRecipe(List<Recipe> aList, boolean aShapeless, Recipe aRecipe) {
		if (aList.contains(aRecipe)) return false;
		aRecipe.addToLists(aList);
		return true;
	}
	
	@SuppressWarnings("deprecation")
	public ItemStack[] applyOreDict(ItemStack stack) {
		if (stack != null) {
			int[] ids = OreDictionary.getOreIDs(stack);
			if (ids.length > 0) {
				Set<ItemStack> stacks = new HashSet<>();
				for (int i : ids) {
					stacks.addAll(OreDictionary.getOres(i));
				}
				
				return stacks.toArray(new ItemStack[stacks.size()]);
			}
		}
		
		return new ItemStack[] {stack};
	}
	
	public Recipe(ItemStack aInput1, ItemStack aInput2, ItemStack aOutput1, ItemStack aOutput2, ItemStack aOutput3, ItemStack aOutput4, int aDuration, int aEUt, int aStartEU) {
//		aInput1  = GT_OreDictUnificator.get(true, aInput1);
//		aInput2  = GT_OreDictUnificator.get(true, aInput2);
//		aOutput1 = GT_OreDictUnificator.get(true, aOutput1);
//		aOutput2 = GT_OreDictUnificator.get(true, aOutput2);
//		aOutput3 = GT_OreDictUnificator.get(true, aOutput3);
//		aOutput4 = GT_OreDictUnificator.get(true, aOutput4);
//		/*
//		 * Wtf gregorious, what the purpose of this?
//		 */
//		if (aInput1 != null && aInput1.getItemDamage() != GregTech_API.ITEM_WILDCARD_DAMAGE) {
//			if (GT_Utility.areStacksEqual(aInput1, aOutput1)) {
//				if (aInput1.stackSize >= aOutput1.stackSize) {
//					aInput1.stackSize -= aOutput1.stackSize;
//					aOutput1 = null;
//				} else {
//					aOutput1.stackSize -= aInput1.stackSize;
//				}
//			}
//			if (GT_Utility.areStacksEqual(aInput1, aOutput2)) {
//				if (aInput1.stackSize >= aOutput2.stackSize) {
//					aInput1.stackSize -= aOutput2.stackSize;
//					aOutput2 = null;
//				} else {
//					aOutput2.stackSize -= aInput1.stackSize;
//				}
//			}
//			if (GT_Utility.areStacksEqual(aInput1, aOutput3)) {
//				if (aInput1.stackSize >= aOutput3.stackSize) {
//					aInput1.stackSize -= aOutput3.stackSize;
//					aOutput3 = null;
//				} else {
//					aOutput3.stackSize -= aInput1.stackSize;
//				}
//			}
//			if (GT_Utility.areStacksEqual(aInput1, aOutput4)) {
//				if (aInput1.stackSize >= aOutput4.stackSize) {
//					aInput1.stackSize -= aOutput4.stackSize;
//					aOutput4 = null;
//				} else {
//					aOutput4.stackSize -= aInput1.stackSize;
//				}
//			}
//		}
//		
//		if (aInput2 != null && aInput2.getItemDamage() != GregTech_API.ITEM_WILDCARD_DAMAGE) {
//			if (GT_Utility.areStacksEqual(aInput2, aOutput1)) {
//				assert aOutput1 != null;
//				if (aInput2.stackSize >= aOutput1.stackSize) {
//					aInput2.stackSize -= aOutput1.stackSize;
//					aOutput1 = null;
//				} else {
//					aOutput1.stackSize -= aInput2.stackSize;
//				}
//			}
//			if (GT_Utility.areStacksEqual(aInput2, aOutput2)) {
//				assert aOutput2 != null;
//				if (aInput2.stackSize >= aOutput2.stackSize) {
//					aInput2.stackSize -= aOutput2.stackSize;
//					aOutput2 = null;
//				} else {
//					aOutput2.stackSize -= aInput2.stackSize;
//				}
//			}
//			if (GT_Utility.areStacksEqual(aInput2, aOutput3)) {
//				assert aOutput3 != null;
//				if (aInput2.stackSize >= aOutput3.stackSize) {
//					aInput2.stackSize -= aOutput3.stackSize;
//					aOutput3 = null;
//				} else {
//					aOutput3.stackSize -= aInput2.stackSize;
//				}
//			}
//			if (GT_Utility.areStacksEqual(aInput2, aOutput4)) {
//				assert aOutput4 != null;
//				if (aInput2.stackSize >= aOutput4.stackSize) {
//					aInput2.stackSize -= aOutput4.stackSize;
//					aOutput4 = null;
//				} else {
//					aOutput4.stackSize -= aInput2.stackSize;
//				}
//			}
//		}
//		
//		for (byte i = 64; i > 1; i--) if (aDuration / i > 0) {
//			if (aInput1  == null || aInput1 .stackSize % i == 0)
//			if (aInput2  == null || aInput2 .stackSize % i == 0)
//			if (aOutput1 == null || aOutput1.stackSize % i == 0)
//			if (aOutput2 == null || aOutput2.stackSize % i == 0)
//			if (aOutput3 == null || aOutput3.stackSize % i == 0)
//			if (aOutput4 == null || aOutput4.stackSize % i == 0) {
//				if (aInput1  != null) aInput1 .stackSize /= i;
//				if (aInput2  != null) aInput2 .stackSize /= i;
//				if (aOutput1 != null) aOutput1.stackSize /= i;
//				if (aOutput2 != null) aOutput2.stackSize /= i;
//				if (aOutput3 != null) aOutput3.stackSize /= i;
//				if (aOutput4 != null) aOutput4.stackSize /= i;
//				aDuration /= i;
//			}
//		}
		
		if (aInput1 == null) {
			mInputs = new ItemStack [0][];
		} else if (aInput2 == null) {
			mInputs = new ItemStack[][]{ applyOreDict(aInput1) }; 
		} else {
			mInputs = new ItemStack[][] { applyOreDict(aInput1), applyOreDict(aInput2)};
		}
		
		mOutputs = new ItemStack[] {aOutput1, aOutput2, aOutput3, aOutput4};
		mDuration = aDuration;
		mStartEU = aStartEU;
		mEUt = aEUt;
		
//		checkCellBalance();
	}
	
	public Recipe(ItemStack aInput1, ItemStack aOutput1, int aStartEU, int aType) {
		this(aInput1, aOutput1, null, null, null, aStartEU, aType);
	}
	
	// aStartEU = EU per Liter! If there is no Liquid for this Object, then it gets multiplied with 1000!
	public Recipe(ItemStack aInput1, ItemStack aOutput1, ItemStack aOutput2, ItemStack aOutput3, ItemStack aOutput4, int aStartEU, int aType) {
		this(aInput1, null, aOutput1, aOutput2, aOutput3, aOutput4, 0, 0, Math.max(1, aStartEU));
		
		if (mInputs.length > 0 && aStartEU > 0) {
			switch (aType) {
			// Diesel Generator
			case 0:
				addToLists(RecipeMaps.sDieselFuels);
				break;
			// Gas Turbine
			case 1:
				addToLists(RecipeMaps.sTurbineFuels);
				break;
			// Thermal Generator
			case 2:
				addToLists(RecipeMaps.sHotFuels);
				break;
			// Plasma Generator
			case 4:
				addToLists(RecipeMaps.sPlasmaFuels);
				break;
			// Magic Generator
			case 5:
				addToLists(RecipeMaps.sMagicFuels);
				break;
			// Fluid Generator. Usually 3. Every wrong Type ends up in the Semifluid Generator
			default:
				addToLists(RecipeMaps.sDenseLiquidFuels);
				break;
			}
		}
	}
	
	public Recipe(ItemStack aInput1, ItemStack aInput2, ItemStack aOutput1, int aDuration, int aEUt, int aStartEU) {
		this(aInput1, aInput2, aOutput1, null, null, null, Math.max(aDuration, 1), aEUt, Math.max(Math.min(aStartEU, 160000000), 0));
		if (mInputs.length > 1 && findEqualRecipe(true, false, sFusionRecipes, aInput1, aInput2) == null) {
			addToLists(sFusionRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, ItemStack aInput2, ItemStack aOutput1, ItemStack aOutput2, ItemStack aOutput3, ItemStack aOutput4, int aDuration) {
		this(aInput1, aInput2, aOutput1, aOutput2, aOutput3, aOutput4, Math.max(aDuration, 1), 5, 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(false, false, sCentrifugeRecipes, aInput1, aInput2) == null) {
			addToLists(sCentrifugeRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, ItemStack aInput2, ItemStack aOutput1, ItemStack aOutput2, ItemStack aOutput3, ItemStack aOutput4, int aDuration, int aEUt) {
		this(aInput1, aInput2, aOutput1, aOutput2, aOutput3, aOutput4, Math.max(aDuration, 1), Math.max(aEUt, 1), 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(false, false, sElectrolyzerRecipes, aInput1, aInput2) == null) {
			addToLists(sElectrolyzerRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, ItemStack aOutput1, ItemStack aOutput2, int aDuration, int aEUt) {
		this(aInput1, null, aOutput1, aOutput2, null, null, aDuration, aEUt, 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(true, false, sLatheRecipes, aInput1) == null) {
			addToLists(sLatheRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, int aDuration, ItemStack aOutput1, int aEUt) {
		this(aInput1, null, aOutput1, null, null, null, aDuration, aEUt, 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(true, false, sCutterRecipes, aInput1) == null) {
			addToLists(sCutterRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, ItemStack aInput2, ItemStack aOutput1, ItemStack aOutput2, ItemStack aOutput3) {
		this(aInput1, aInput2, aOutput1, aOutput2, aOutput3, null, 200*aInput1.stackSize, 30, 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(false, false, sSawmillRecipes, aInput1, aInput2) == null) {
			addToLists(sSawmillRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, ItemStack aInput2, ItemStack aOutput1, ItemStack aOutput2, ItemStack aOutput3, ItemStack aOutput4) {
		this(aInput1, aInput2, aOutput1, aOutput2, aOutput3, aOutput4, 100*aInput1.stackSize, 120, 0);
		if (mInputs.length > 0 && aInput2 != null && mOutputs[0] != null && findEqualRecipe(false, false, sGrinderRecipes, aInput1, aInput2) == null) {
			addToLists(sGrinderRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, int aCellAmount, ItemStack aOutput1, ItemStack aOutput2, ItemStack aOutput3, ItemStack aOutput4, int aDuration, int aEUt) {
		this(aInput1, aCellAmount>0?GT_Items.Cell_Empty.get(Math.min(64, Math.max(1, aCellAmount))):null, aOutput1, aOutput2, aOutput3, aOutput4, Math.max(aDuration, 1), Math.max(aEUt, 1), 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(false, false, sDistillationRecipes, aInput1, aCellAmount>0?GT_Items.Cell_Empty.get(Math.min(64, Math.max(1, aCellAmount))):null) == null) {
			addToLists(sDistillationRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, ItemStack aInput2, ItemStack aOutput1, ItemStack aOutput2, int aDuration, int aEUt, int aLevel) {
		this(aInput1, aInput2, aOutput1, aOutput2, null, null, Math.max(aDuration, 1), Math.max(aEUt, 1), aLevel > 0 ? aLevel : 100);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(true, false, sBlastRecipes, aInput1, aInput2) == null) {
			addToLists(sBlastRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, int aInput2, ItemStack aOutput1, ItemStack aOutput2) {
		this(aInput1, GT_ModHandler.getIC2Item("industrialTnt", aInput2>0?aInput2<64?aInput2:64:1, new ItemStack(Blocks.tnt, aInput2>0?aInput2<64?aInput2:64:1)), aOutput1, aOutput2, null, null, 20, 30, 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(false, false, sImplosionRecipes, aInput1, GT_ModHandler.getIC2Item("industrialTnt", aInput2>0?aInput2<64?aInput2:64:1, new ItemStack(Blocks.tnt, aInput2>0?aInput2<64?aInput2:64:1))) == null) {
			addToLists(sImplosionRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, int aEUt, int aDuration, ItemStack aOutput1) {
		this(aInput1, null, aOutput1, null, null, null, Math.max(aDuration, 1), Math.max(aEUt, 1), 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(true, false, sWiremillRecipes, aInput1) == null) {
			addToLists(sWiremillRecipes);
		}
	}
	
	public Recipe(int aEUt, int aDuration, ItemStack aInput1, ItemStack aOutput1) {
		this(aInput1, GT_Items.Circuit_Integrated.getWithDamage(0, aInput1.stackSize), aOutput1, null, null, null, Math.max(aDuration, 1), Math.max(aEUt, 1), 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(false, false, sBenderRecipes, aInput1) == null) {
			addToLists(sBenderRecipes);
		}
	}
	
	public Recipe(int aEUt, int aDuration, ItemStack aInput1, ItemStack aShape, ItemStack aOutput1) {
		this(aInput1, aShape, aOutput1, null, null, null, Math.max(aDuration, 1), Math.max(aEUt, 1), 0);
		if (mInputs.length > 1 && mOutputs[0] != null && findEqualRecipe(false, false, sExtruderRecipes, aInput1) == null) {
			addToLists(sExtruderRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, int aEUt, ItemStack aInput2, int aDuration, ItemStack aOutput1) {
		this(aInput1, aInput2, aOutput1, null, null, null, Math.max(aDuration, 1), Math.max(aEUt, 1), 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(true, false, sAssemblerRecipes, aInput1, aInput2) == null) {
			addToLists(sAssemblerRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, ItemStack aInput2, int aEUt, int aDuration, ItemStack aOutput1) {
		this(aInput1, aInput2, aOutput1, null, null, null, Math.max(aDuration, 1), Math.max(aEUt, 1), 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(true, false, sAlloySmelterRecipes, aInput1, aInput2) == null) {
			addToLists(sAlloySmelterRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, int aEUt, ItemStack aInput2, int aDuration, ItemStack aOutput1, ItemStack aOutput2) {
		this(aInput1, aInput2, aOutput1, aOutput2, null, null, Math.max(aDuration, 1), Math.max(aEUt, 1), 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(true, false, sCannerRecipes, aInput1, aInput2) == null) {
			addToLists(sCannerRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, ItemStack aOutput1, int aDuration) {
		this(aInput1, null, aOutput1, null, null, null, Math.max(aDuration, 1), 120, 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(true, false, sVacuumRecipes, aInput1) == null) {
			addToLists(sVacuumRecipes);
		}
	}
	
	public Recipe(ItemStack aInput1, ItemStack aInput2, ItemStack aOutput1, int aDuration) {
		this(aInput1, aInput2, aOutput1, null, null, null, Math.max(aDuration, 1), 30, 0);
		if (mInputs.length > 0 && mOutputs[0] != null && findEqualRecipe(true, false, sChemicalRecipes, aInput1) == null) {
			addToLists(sChemicalRecipes);
		}
	}
	
	@Override
	public int hashCode() {
		int res = 0;
		for (ItemStack[] stacks : mInputs)
			res += GT_Utility.stackArrayToInt(stacks);
		return (mDuration * mEUt * mStartEU) + res + GT_Utility.stackArrayToInt(mOutputs);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Recipe) {
			Recipe r = (Recipe) o;
			return r == this ||
					(r.mDuration == this.mDuration &&
					r.mEUt == this.mEUt &&
					GT_Utility.doesStackArraysSame(r.mOutputs, this.mOutputs)) &&
					GT_Utility.doesRecipeInputsSame(r.mInputs, this.mInputs);
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return "Recipe[inputs=" + mInputs.length + ",outputs=" + mOutputs.length + ",EUt=" + mEUt + ",duration=" + mDuration + ",startEU=" + mStartEU + ",enabled=" + mEnabled + "]";
	}
}