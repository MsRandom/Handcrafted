package earth.terrarium.handcrafted.common.blocks;

import com.mojang.serialization.MapCodec;
import earth.terrarium.handcrafted.common.blockentities.ContainerBlockEntity;
import earth.terrarium.handcrafted.common.blocks.base.Hammerable;
import earth.terrarium.handcrafted.common.blocks.base.properties.DirectionalBlockProperty;
import earth.terrarium.handcrafted.common.constants.ConstantComponents;
import earth.terrarium.handcrafted.common.registry.ModItems;
import earth.terrarium.handcrafted.common.registry.ModSoundEvents;
import earth.terrarium.handcrafted.common.utils.TooltipUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DrawerBlock extends HorizontalDirectionalBlock implements Hammerable, EntityBlock {
    public static final MapCodec<DrawerBlock> CODEC = simpleCodec(DrawerBlock::new);
    public static final IntegerProperty TYPE = IntegerProperty.create("type", 1, 4);
    public static final EnumProperty<DirectionalBlockProperty> SHAPE = EnumProperty.create("shape", DirectionalBlockProperty.class);

    public DrawerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(TYPE, 1)
            .setValue(SHAPE, DirectionalBlockProperty.SINGLE)
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE, SHAPE);
    }

    @Override
    public void onHammer(Level level, BlockPos pos, BlockState state, Direction side, Player user, Vec3 hitPos) {
        if (level.isClientSide()) return;
        level.setBlockAndUpdate(pos, state.cycle(TYPE));
        level.playSound(null, pos, ModSoundEvents.HAMMER_WOOD.get(), SoundSource.BLOCKS, 1, 1);
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return direction.getAxis().isHorizontal() ? state.setValue(SHAPE, DiningBenchBlock.getShape(this, state.getValue(FACING), level, currentPos)) : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite());
        return state.setValue(SHAPE, DiningBenchBlock.getShape(this, state.getValue(FACING), context.getLevel(), pos));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.CONSUME_PARTIAL;
        if (stack.is(ModItems.HAMMER.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.getBlockEntity(pos) instanceof ContainerBlockEntity container) {
            player.openMenu(container);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ContainerBlockEntity(pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)level.getBlockEntity(pos));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        TooltipUtils.addDescriptionComponent(tooltipComponents, ConstantComponents.HAMMER_USE_LOOK);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof ContainerBlockEntity container) {
                if (container.getContainerSize() > 0) {
                    Containers.dropContents(level, pos, container);
                    level.updateNeighbourForOutputSignal(pos, this);
                }
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }
}
