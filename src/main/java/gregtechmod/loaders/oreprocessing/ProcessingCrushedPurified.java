package gregtechmod.loaders.oreprocessing;

import java.util.List;

import gregtechmod.api.enums.Materials;
import gregtechmod.api.enums.OrePrefixes;
import gregtechmod.api.enums.SubTag;
import gregtechmod.api.interfaces.IOreRecipeRegistrator;
import gregtechmod.api.util.GT_ModHandler;
import gregtechmod.api.util.GT_OreDictUnificator;
import gregtechmod.api.util.GT_Utility;
import gregtechmod.api.util.OreDictEntry;
import gregtechmod.common.recipe.RecipeMaps;
import net.minecraft.item.ItemStack;

public class ProcessingCrushedPurified implements IOreRecipeRegistrator {

	public ProcessingCrushedPurified() {
		OrePrefixes.crushedPurified.add(this);
	}

	public void registerOre(OrePrefixes aPrefix, List<OreDictEntry> entries) {
		for (OreDictEntry entry : entries) {
			Materials aMaterial = this.getMaterial(aPrefix, entry);
			if (this.isExecutable(aPrefix, aMaterial)) {
				for (ItemStack aStack : entry.ores) {
					GT_ModHandler.addPulverisationRecipe(GT_Utility.copyAmount(1L, aStack),
					GT_OreDictUnificator.get(OrePrefixes.dustPure, aMaterial, GT_OreDictUnificator.get(OrePrefixes.dust, aMaterial, 1L), 1L), GT_OreDictUnificator.get(OrePrefixes.dust, GT_Utility.selectItemInList(1, aMaterial, aMaterial.mOreByProducts), 1L), 10, false);
					if (!aMaterial.contains(SubTag.NO_SMELTING)) {
						RecipeMaps.CENTRIFUGE.factory().EUt(5)
								.duration(Math.max(1, aMaterial.getMass() * 16))
								.input(GT_Utility.copyAmount(1L, aStack))
								.outputs(GT_OreDictUnificator.get(OrePrefixes.crushedCentrifuged, aMaterial, 1L), GT_OreDictUnificator.get(OrePrefixes.dustTiny, GT_Utility.selectItemInList(1, aMaterial, aMaterial.mOreByProducts), 1L))
								.buildAndRegister();
					}
				}
			}
		}
	}
}
