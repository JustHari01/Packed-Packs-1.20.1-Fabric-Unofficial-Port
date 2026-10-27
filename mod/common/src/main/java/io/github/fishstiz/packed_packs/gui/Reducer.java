package io.github.fishstiz.packed_packs.gui;

import com.google.common.primitives.Ints;
import io.github.fishstiz.fidgetz.v0.utils.CollectionUtils;
import io.github.fishstiz.packed_packs.config.Profile;
import io.github.fishstiz.packed_packs.gui.actions.mutations.Mutation;
import io.github.fishstiz.packed_packs.gui.components.SortOptions;
import io.github.fishstiz.packed_packs.gui.states.PackListKey;
import io.github.fishstiz.packed_packs.gui.states.PackListType;
import io.github.fishstiz.packed_packs.util.PackListComputedUtils;
import io.github.fishstiz.packed_packs.gui.states.Query;
import io.github.fishstiz.packed_packs.gui.states.ActiveAction;
import io.github.fishstiz.packed_packs.gui.states.PackListState;
import io.github.fishstiz.packed_packs.gui.states.PackedPacksState;
import io.github.fishstiz.packed_packs.gui.states.ProfilesState;
import io.github.fishstiz.packed_packs.gui.actions.mutations.PackListMutation;
import io.github.fishstiz.packed_packs.gui.actions.mutations.ProfileMutation;
import io.github.fishstiz.packed_packs.pack.PackNode;
import io.github.fishstiz.packed_packs.pack.PackSelection;
import io.github.fishstiz.packed_packs.gui.states.ProfileSelection;
import io.github.fishstiz.packed_packs.util.ToIntTriFunction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.fishstiz.packed_packs.util.PackListComputedUtils.*;

public final class Reducer {
    public static PackedPacksState reduce(PackedPacksState state, Mutation mutation) {
        if (mutation instanceof Mutation.Reset reset) {
                PackListState available = state.available();
                PackListState enabled = state.enabled();
                return new PackedPacksState(
                        available.with(
                                reset.packs().disabled(),
                                Collections.emptyList(),
                                available.query().withSearch(""),
                                state.profiles(),
                                state.devMode()
                        ),
                        enabled.with(
                                reset.packs().enabled(),
                                Collections.emptyList(),
                                enabled.query().withSearch(""),
                                state.profiles(),
                                state.devMode()
                        ),
                        state.profiles(),
                        state.lastTarget().head(),
                        null,
                        state.devMode()
                );
        }
        if (mutation instanceof Mutation.DevModeToggled) {
                PackedPacksState newState = state.withDevMode(!state.devMode());
                return newState.withPackLists(
                        refreshList(newState.available(), newState.profiles(), newState.devMode()),
                        refreshList(newState.enabled(), newState.profiles(), newState.devMode())
                );
        }
        if (mutation instanceof Mutation.PackRenaming renaming) {
                ActiveAction.RenamingPack renamingState = state.renamingPack();
                if (renamingState != null) {
                    return state.withAction(renamingState.withLoading(renaming.loading()));
                }
                return state;
        }
        if (mutation instanceof PackListMutation packListMutation) {
            if (packListMutation instanceof PackListMutation.Local local) {
                return switch (local.srcList().type()) {
                    case AVAILABLE -> {
                        PackListState newAvailable = reduceList(state.available(), 0, local, state.profiles(), state.devMode());
                        yield newAvailable == state.available() ? state : state.withAvailable(newAvailable, local.srcList());
                    }
                    case ENABLED -> {
                        PackListState newEnabled = reduceList(state.enabled(), 0, local, state.profiles(), state.devMode());
                        yield newEnabled == state.available() ? state : state.withEnabled(newEnabled, local.srcList());
                    }
                };
            }
            if (packListMutation instanceof PackListMutation.Cross cross) {
                return reduceList(state, cross);
            }
        }
        if (mutation instanceof ProfileMutation profileMutation) {
            return reduceProfile(state, profileMutation);
        }
        return state;
    }

    private static PackListState refreshList(PackListState state, ProfileSelection profiles, boolean devMode) {
        state = state.withQuery(state.query(), profiles, devMode);

        PackListState currentParent = state;
        PackListState currentFolder = state.folder();
        for (boolean head = true; currentFolder != null; head = false) {
            currentParent = currentParent.withFolder(
                    currentFolder.withQuery(currentFolder.query(), profiles, devMode),
                    profiles,
                    devMode
            );

            if (head) state = currentParent;
            currentParent = currentParent.folder();
            currentFolder = currentParent == null ? null : currentParent.folder();
        }

        return state;
    }

    private static PackListState reduceList(
            PackListState state,
            int depth,
            PackListMutation.Local mutation,
            ProfileSelection profiles,
            boolean devMode
    ) {
        if (depth == mutation.srcList().depth()) {
            return reduceList(state, mutation, profiles, devMode);
        }
        if (state.folder() == null || mutation.srcList().depth() < 0) {
            return state;
        }
        PackListState newFolder = reduceList(state.folder(), depth + 1, mutation, profiles, devMode);
        return newFolder == state.folder() ? state : state.withFolder(newFolder, profiles, devMode);
    }

    private static PackListState reduceList(
            PackListState state,
            PackListMutation.Local mutation,
            ProfileSelection profiles,
            boolean devMode
    ) {
        if (mutation instanceof PackListMutation.Searched searched) {
            return state.withQuery(state.query().withSearch(searched.search()), profiles, devMode);
        }
        if (mutation instanceof PackListMutation.Sorted sorted) {
            return state.withQuery(state.query().withSort(sorted.sort()), profiles, devMode);
        }
        if (mutation instanceof PackListMutation.IncompatibleHidden incompatibleHidden) {
            return state.withQuery(state.query().withHideIncompatible(incompatibleHidden.hidden()), profiles, devMode);
        }
        if (mutation instanceof PackListMutation.Selected selected) {
                PackNode pack = selected.pack();
                if (!state.visiblePacks().contains(pack)) return state;
                List<PackNode> newSelection = new ObjectArrayList<>(state.selectedPacks());
                newSelection.remove(pack);
                newSelection.add(pack);
                return state.withSelection(newSelection);
        }
        if (mutation instanceof PackListMutation.SelectedMultiple selected) {
            return state.withSelection(selected.packs());
        }
        if (mutation instanceof PackListMutation.SelectedExclusively selected) {
                PackNode pack = selected.pack();
                if (!state.visiblePacks().contains(pack)) return state;
                return state.withSelection(List.of(pack));
        }
        if (mutation instanceof PackListMutation.SelectionToggled selected) {
                PackNode pack = selected.pack();
                if (!state.visiblePacks().contains(pack)) return state;
                List<PackNode> newSelection = new ObjectArrayList<>(state.selectedPacks());
                if (!newSelection.remove(pack)) newSelection.add(pack);
                return state.withSelection(newSelection);
        }
        if (mutation instanceof PackListMutation.SelectedRange selected) {
                if (state.selectedPacks().isEmpty()) {
                    return state.withSelection(List.of(selected.pack()));
                }

                PackNode anchor = io.github.fishstiz.fidgetz.v0.utils.CollectionUtils.last(state.selectedPacks());
                int anchorIndex = state.visiblePacks().indexOf(anchor);
                int targetIndex = state.visiblePacks().indexOf(selected.pack());
                int[] indices = indicesOf(state.visiblePacks(), state.selectedPacks());
                Arrays.sort(indices);

                if (!(Ints.contains(indices, -1) || hasGap(indices, true)) && indices.length > 0) {
                    if (indices[0] == anchorIndex) {
                        anchor = state.visiblePacks().get(indices[indices.length - 1]);
                    } else if (indices[indices.length - 1] == anchorIndex) {
                        anchor = state.visiblePacks().get(indices[0]);
                    }
                }
                List<PackNode> newSelection = new ObjectArrayList<>(state.selectedPacks());
                int start = state.visiblePacks().indexOf(anchor);
                if (targetIndex != -1 && start != -1) {
                    newSelection.clear();
                    for (int i = Math.min(targetIndex, start); i <= Math.max(targetIndex, start); i++) {
                        PackNode entry = state.visiblePacks().get(i);
                        if (entry != null && entry != selected.pack() && state.visiblePacks().contains(entry)) {
                            newSelection.remove(entry);
                            newSelection.add(entry);
                        }
                    }
                }

                newSelection.add(selected.pack());
                return state.withSelection(newSelection);
        }
        if (mutation instanceof PackListMutation.SelectedAll selected) {
                ObjectLinkedOpenHashSet<PackNode> newSelection = new ObjectLinkedOpenHashSet<>(state.visiblePacks());
                if (selected.pack() != null) newSelection.addAndMoveToLast(selected.pack());
                return state.withSelection(newSelection);
        }
        if (mutation instanceof PackListMutation.Moved moved) {
                List<PackNode> ordered = sortByOrderOf(state.visiblePacks(), moved.packs());
                List<PackNode> newPacks = new ObjectArrayList<>(state.packs());
                int to = moved.index();
                int insertOffset = 0;
                for (PackNode pack : ordered) {
                    int previous = newPacks.indexOf(pack);
                    boolean isBeforeTo = previous != -1 && previous < to;
                    int target = isBeforeTo ? to - 1 : to + insertOffset;
                    newPacks.remove(pack);
                    newPacks.add(net.minecraft.util.Mth.clamp(target, 0, newPacks.size()), pack);
                    if (!isBeforeTo) insertOffset++;
                }
                return newPacks.equals(state.packs()) ? state : state.withPacks(newPacks, profiles, devMode);
        }
        if (mutation instanceof PackListMutation.MovedOnce moved) {
                List<PackNode> newPacks = moved.packs().isEmpty()
                        ? state.packs()
                        : movePacks(state, moved.packs(), moved.upwards(), profiles);

                return state.withPacksAndSelectedLast(newPacks, moved.srcPack(), profiles, devMode);
        }
        if (mutation instanceof PackListMutation.FolderOpened opened) {
            return state.withFolder(
                    PackListState.folder(opened.pack(), opened.locked(), state.query().withSort(SortOptions.NONE))
                            .withPacks(opened.children(), profiles, devMode),
                    profiles,
                    devMode
            );
        }
        if (mutation instanceof PackListMutation.FolderClosed) {
            return state.withFolder(null, profiles, devMode);
        }
        return state;
    }


    private static List<PackNode> movePacks(
            PackListState state,
            Collection<PackNode> payload,
            boolean up,
            ProfileSelection profiles
    ) {
        Set<PackNode> validPacks = new ObjectOpenHashSet<>(state.packs());
        List<PackNode> newPacks = new ObjectArrayList<>(state.packs());
        List<PackNode> sorted;
        ToIntTriFunction<List<PackNode>, PackNode, ProfileSelection> indexFn;

        if (up) {
            sorted = sortByOrderOf(state.visiblePacks(), payload);
            indexFn = PackListComputedUtils::getMoveUpIndex;
        } else {
            sorted = io.github.fishstiz.fidgetz.v0.utils.CollectionUtils.reversed(sortByOrderOf(state.visiblePacks(), payload));
            indexFn = PackListComputedUtils::getMoveDownIndex;
        }

        for (int i = 0; i < sorted.size(); i++) {
            PackNode pack = sorted.get(i);
            if (!validPacks.contains(pack)) continue;
            int targetIndex = indexFn.applyAsInt(newPacks, pack, profiles);
            if (targetIndex > -1 && targetIndex < newPacks.size() && !profiles.isPackFixed(pack)) {
                newPacks.remove(pack);
                newPacks.add(targetIndex, pack);
            } else if (i == 0) {
                return state.packs();
            }
        }

        return newPacks;
    }

    private static PackListState updateHeadList(
            PackListState headList,
            int depth,
            PackListState state,
            ProfileSelection profiles,
            boolean devMode
    ) {
        if (depth == 0) {
            return state;
        }
        if (headList.folder() == null) {
            return headList;
        }
        return headList.withFolder(
                updateHeadList(headList.folder(), depth - 1, state, profiles, devMode),
                profiles,
                devMode
        );
    }

    private static PackedPacksState updateHeadList(
            PackedPacksState state,
            PackListState head,
            PackListType type,
            PackListKey actionSrc
    ) {
        return switch (type) {
            case AVAILABLE -> state.withAvailable(head, actionSrc);
            case ENABLED -> state.withEnabled(head, actionSrc);
        };
    }

    private static PackedPacksState transfer(PackedPacksState state, PackListMutation.Transfer transfer) {
        PackListKey destKey = transfer.srcList().type().available()
                ? PackListKey.enabled()
                : state.getTailKey(PackListType.AVAILABLE);

        PackListState destState = Objects.requireNonNullElse(state.getList(destKey), PackListState.empty());
        List<PackNode> newDestPacks = new ObjectArrayList<>(destState.packs());
        List<PackNode> newDestSelection = new ObjectArrayList<>(transfer.packs().size());
        Set<String> newDestIds = newDestPacks.stream()
                .map(PackNode::id)
                .collect(Collectors.toCollection(ObjectOpenHashSet::new));

        PackNode srcPack = transfer.srcPack();
        PackListState srcState = Objects.requireNonNullElse(state.getList(transfer.srcList()), PackListState.empty());
        Set<PackNode> newSrcPacks = new ObjectLinkedOpenHashSet<>(srcState.packs());

        int clampedIndex = net.minecraft.util.Mth.clamp(transfer.index(), 0, newDestPacks.size());

        for (PackNode pack : transfer.packs()) {
            newSrcPacks.remove(pack); // do not validate on add using remove as the payload may contain nested packs
            if (newDestIds.add(pack.id())) {
                newDestPacks.add(clampedIndex, pack);
                if (srcPack == null || !pack.id().equals(srcPack.id()) && !destState.selectedPacks().contains(pack)) {
                    newDestSelection.add(pack);
                }
            }
        }

        if (srcPack != null) {
            newDestSelection.add(srcPack);
        }

        Collection<PackNode> newSrcSelection = srcPack != null && srcState.selectedPacks().contains(srcPack)
                ? srcState.selectedPacks()
                : Collections.emptyList();

        PackListState srcHeadList = state.getHeadList(transfer.srcList().type());
        PackListState newSrcState = srcState == PackListState.empty() ? srcHeadList : updateHeadList(
                srcHeadList,
                transfer.srcList().depth(),
                srcState.with(List.copyOf(newSrcPacks), newSrcSelection, state.profiles(), state.devMode()),
                state.profiles(),
                state.devMode()
        );

        PackListState destHeadList = state.getHeadList(destKey.type());
        PackListState newDestState = destState == PackListState.empty() ? destHeadList : updateHeadList(
                destHeadList,
                destKey.depth(),
                destState.with(newDestPacks, newDestSelection, state.profiles(), state.devMode()),
                state.profiles(),
                state.devMode()
        );

        PackListState disabled;
        PackListState enabled;

        if (transfer.srcList().type().available()) {
            disabled = newSrcState;
            enabled = newDestState;
        } else {
            disabled = newDestState;
            enabled = newSrcState;
        }

        return state.withPackLists(disabled, enabled, transfer.srcList().head());
    }

    private static PackedPacksState reduceList(PackedPacksState state, PackListMutation.Cross mutation) {
        if (mutation instanceof PackListMutation.Enabled enabled) {
                if (enabled.srcList().type().enabled()) {
                    return state;
                }

                PackedPacksState newState = transfer(state, enabled);
                if (newState.enabled().folder() == null) {
                    return newState;
                }

                return newState.withEnabled(
                        newState.enabled().withFolder(null, state.profiles(), state.devMode()),
                        newState.lastTarget()
                );
        }
        if (mutation instanceof PackListMutation.Disabled disabled) {
            return transfer(
                    state.enabled().folder() == null ? state : state.withEnabled(
                            state.enabled().withFolder(null, state.profiles(), state.devMode()),
                            state.lastTarget()
                    ),
                    disabled.srcList().type().enabled()
                            ? disabled
                            : new PackListMutation.Disabled(PackListKey.enabled(), null, disabled.packs())
            );
        }
        if (mutation instanceof PackListMutation.Dragged dragged) {
                PackListState srcListState = state.getList(dragged.srcList());

                ActiveAction.Dragging draggingAction = new ActiveAction.Dragging(
                        dragged.srcList(),
                        srcListState != null && srcListState.module(),
                        dragged.srcPack(),
                        dragged.packs()
                );

                PackListKey enabledTailKey = state.getTailKey(PackListType.ENABLED);
                PackListState enabledListState = Objects.requireNonNull(state.getList(enabledTailKey));

                if (!enabledListState.query().hasQuery()) {
                    return state.withAction(draggingAction);
                }

                PackListState newEnabledState = updateHeadList(
                        state.enabled(),
                        enabledTailKey.depth(),
                        enabledListState.withQuery(Query.empty(), state.profiles(), state.devMode()),
                        state.profiles(),
                        state.devMode()
                );

                return state.withEnabled(newEnabledState, draggingAction.src()).withAction(draggingAction);
        }
        if (mutation instanceof PackListMutation.Dropped dropped) {
                ActiveAction.Dragging dragging = state.dragging();
                if (dragging == null) return state;

                PackListKey dest = dropped.targetList();
                PackedPacksState newState = state.withAction(null);
                if (dest == null) return newState;

                PackListState destListState = newState.getList(dest);
                if (destListState == null) return newState;

                PackListKey srcList = dropped.srcList();
                PackListState srcListState = newState.getList(srcList);
                if (srcListState == null) return newState;

                int index = dropped.index();
                if (!canDrop(dragging, dest, destListState, index, state.profiles())) {
                    return newState;
                }

                Collection<PackNode> payload = dropped.packs();
                int position = clampIndex(destListState, getAbsoluteIndex(destListState, index), state.profiles());
                if (srcList.equals(dest)) {
                    List<PackNode> packs = new ObjectArrayList<>(payload.size());
                    CollectionUtils.addIf(packs, payload, p -> canDrag(srcList, srcListState.module(), p, state.profiles()));

                    if (!packs.isEmpty()) {
                        return reduce(newState, new PackListMutation.Moved(dest, dragging.srcPack(), packs, position));
                    }
                } else if (canTransfer(srcList, srcListState.module(), dragging.srcPack(), state.profiles())) {
                    List<PackNode> packs = new ObjectArrayList<>(payload.size());
                    CollectionUtils.addIf(packs, payload, p -> canTransfer(srcList, false, p, state.profiles()));

                    if (!packs.isEmpty()) {
                        return reduceList(newState, switch (dest.type()) {
                            case AVAILABLE ->
                                    new PackListMutation.Disabled(srcList, dragging.srcPack(), io.github.fishstiz.fidgetz.v0.utils.CollectionUtils.reversed(packs), position);
                            case ENABLED ->
                                    new PackListMutation.Enabled(srcList, dragging.srcPack(), io.github.fishstiz.fidgetz.v0.utils.CollectionUtils.reversed(packs), position);
                        });
                    }
                }
                return newState;
        }
        if (mutation instanceof PackListMutation.ModuleUpdated updated) {
                PackListState srcState = state.getList(updated.srcList());
                if (srcState == null) {
                    return state;
                }

                if (srcState.module() == updated.module()) {
                    return state;
                }

                PackListState newSrcState = srcState.withModule(updated.module(), state.profiles(), state.devMode());
                PackedPacksState newState = updateHeadList(
                        state,
                        updateHeadList(
                                state.getHeadList(updated.srcList().type()),
                                updated.srcList().depth(),
                                newSrcState,
                                state.profiles(),
                                state.devMode()
                        ),
                        updated.srcList().type(),
                        updated.srcList()
                );

                if (!newSrcState.module() || newSrcState.parent() == null || updated.srcList().type().enabled()) {
                    return newState;
                }

                Set<PackNode> newEnabledPacks = new ObjectLinkedOpenHashSet<>(state.enabled().packs());
                List<PackNode> removedPacks = new ObjectArrayList<>();

                newSrcState.parent().visitNodes(node -> {
                    if (newEnabledPacks.remove(node)) {
                        removedPacks.add(node);
                    }
                });

                if (removedPacks.isEmpty()) {
                    return newState;
                }

                Set<PackNode> newChildren = new ObjectLinkedOpenHashSet<>(newSrcState.packs());
                newChildren.addAll(removedPacks);

                return updateHeadList(
                        updateHeadList(
                                newState,
                                updateHeadList(
                                        newState.getHeadList(updated.srcList().type()),
                                        updated.srcList().depth(),
                                        newSrcState.withPacks(List.copyOf(newChildren), state.profiles(), state.devMode()),
                                        newState.profiles(),
                                        newState.devMode()
                                ),
                                updated.srcList().type(),
                                updated.srcList()
                        ),
                        newState.enabled().withPacks(List.copyOf(newEnabledPacks), state.profiles(), state.devMode()),
                        PackListType.ENABLED,
                        updated.srcList()
                );
        }
        if (mutation instanceof PackListMutation.RenameModalOpened opened) {
            return state.withAction(new ActiveAction.RenamingPack(opened.srcList(), opened.pack(), false));
        }
        if (mutation instanceof PackListMutation.RenameModalClosed) {
            return state.withAction(null);
        }
        if (mutation instanceof PackListMutation.AliasesModalOpened opened) {
            return state.withAction(new ActiveAction.EditingAliases(opened.srcList(), opened.srcPack(), opened.aliases()));
        }
        if (mutation instanceof PackListMutation.AliasesModalClosed) {
            return state.withAction(null);
        }
        if (mutation instanceof PackListMutation.RequirementOverridden overridden) {
                Profile selectedProfile = state.profiles().selectedProfile();
                if (selectedProfile == null) return state;

                for (PackNode entry : overridden.packs()) {
                    // just pretend its immutable
                    //noinspection DataFlowIssue
                    selectedProfile = selectedProfile.withRequiredOverride(overridden.required(), entry.id());
                }

                PackedPacksState newState = state.withProfiles(state.profiles().withSelected(selectedProfile));

                if (Boolean.TRUE.equals(overridden.required()) && overridden.srcList().type().available()) {
                    return reduceList(newState, new PackListMutation.Enabled(
                            overridden.srcList(),
                            overridden.srcPack(),
                            io.github.fishstiz.fidgetz.v0.utils.CollectionUtils.reversed(overridden.packs()),
                            0
                    ));
                }

                return newState;
        }
        if (mutation instanceof PackListMutation.PositionOverridden overridden) {
                Profile selectedProfile = state.profiles().selectedProfile();
                if (selectedProfile == null) return state;

                for (PackNode entry : overridden.packs()) {
                    // just pretend its immutable
                    //noinspection DataFlowIssue
                    selectedProfile = selectedProfile.withPositionOverride(overridden.position(), entry.id());
                }

                return state.withProfiles(state.profiles().withSelected(selectedProfile));
        }
        if (mutation instanceof PackListMutation.VisibilityOverridden overridden) {
                Profile selectedProfile = state.profiles().selectedProfile();
                if (selectedProfile == null) return state;

                for (PackNode entry : overridden.packs()) {
                    // just pretend its immutable
                    //noinspection DataFlowIssue
                    selectedProfile = selectedProfile.withHiddenOverride(overridden.hidden(), entry.id());
                }

                return state.withProfiles(state.profiles().withSelected(selectedProfile));
        }
        if (mutation instanceof PackListMutation.OverridesRemoved overridesRemoved) {
                Profile selectedProfile = state.profiles().selectedProfile();
                if (selectedProfile == null) return state;

                for (PackNode entry : overridesRemoved.packs()) {
                    // just pretend its immutable
                    //noinspection DataFlowIssue
                    selectedProfile = selectedProfile
                            .withRequiredOverride(null, entry.id())
                            .withPositionOverride(null, entry.id())
                            .withHiddenOverride(false, entry.id());
                }

                return state.withProfiles(state.profiles().withSelected(selectedProfile));
        }
        return state;
    }

    private static PackedPacksState reduceProfile(PackedPacksState state, ProfileMutation mutation) {
        if (mutation instanceof ProfileMutation.Selected selected) {
                ProfilesState profiles = state.profiles();
                PackSelection packs = selected.packs();
                PackedPacksState newState = state.withProfiles(profiles.withSelected(selected.profile()));

                return newState.withPackLists(
                        state.available().withPacks(packs.disabled(), profiles, state.devMode())
                                .withFolder(null, newState.profiles(), newState.devMode()),
                        state.enabled().withPacks(packs.enabled(), profiles, state.devMode())
                                .withFolder(null, newState.profiles(), newState.devMode())
                );
        }
        if (mutation instanceof ProfileMutation.Deleted delete) {
                List<Profile> newProfileList = new ObjectArrayList<>(state.profiles().profiles());
                newProfileList.remove(delete.profile());
                return state.withProfiles(state.profiles().withProfiles(newProfileList));
        }
        if (mutation instanceof ProfileMutation.DeletedAndReset delete) {
                List<Profile> newProfileList = new ObjectArrayList<>(state.profiles().profiles());
                newProfileList.remove(delete.profile());

                ProfilesState newProfiles = state.profiles().withProfiles(newProfileList).withSelected(null);
                return state.withProfiles(newProfiles).withPackLists(
                        state.available().withPacks(delete.packs().disabled(), newProfiles, state.devMode())
                                .withFolder(null, newProfiles, state.devMode()),
                        state.enabled().withPacks(delete.packs().enabled(), newProfiles, state.devMode())
                                .withFolder(null, newProfiles, state.devMode())
                );
        }
        if (mutation instanceof ProfileMutation.Added added) {
                List<Profile> newProfileList = new ObjectArrayList<>(state.profiles().profiles());
                Profile newProfile = added.profile();
                newProfileList.add(newProfile);
                return reduceProfile(
                        state.withProfiles(state.profiles().withProfiles(newProfileList)),
                        new ProfileMutation.Selected(
                                newProfile,
                                new PackSelection(state.available().packs(), state.enabled().packs())
                        )
                );
        }
        if (mutation instanceof ProfileMutation.DefaultRemoved) {
            return state.withProfiles(state.profiles().withDefault(null));
        }
        if (mutation instanceof ProfileMutation.DefaultChanged defaultChanged) {
                Profile profile = defaultChanged.profile();
                @Nullable PackSelection packs = defaultChanged.packs();
                PackedPacksState newState = state.withProfiles(
                        updateProfileState(state.profiles().withDefault(profile), profile)
                );

                if (Objects.equals(profile, state.profiles().selectedProfile()) || packs == null) {
                    return newState;
                }

                return reduceProfile(newState, new ProfileMutation.Selected(profile, packs));
        }
        if (mutation instanceof ProfileMutation.LockToggled lockToggled) {
            Profile profile = lockToggled.profile();
            return state.withProfiles(updateProfileState(state.profiles(), profile.withLocked(!profile.isLocked())));
        }
        if (mutation instanceof ProfileMutation.Renamed renamed) {
            return state.withProfiles(updateProfileState(state.profiles(), renamed.profile().withName(renamed.name()))
                    .withRenaming(state.profiles().renaming()));
        }
        if (mutation instanceof ProfileMutation.RenamingToggled) {
            return state.withProfiles(state.profiles().withRenaming(!state.profiles().renaming()));
        }
        return state;
    }

    // just pretend profile is immutable, at some point it will be (maybe)
    private static ProfilesState updateProfileState(ProfilesState state, Profile newProfile) {
        Profile selectedProfile = state.selectedProfile();
        if (selectedProfile != null && (selectedProfile == newProfile || newProfile.getId().equals(selectedProfile.getId()))) {
            selectedProfile = newProfile;
        }

        Profile defaultProfile = state.defaultProfile();
        if (defaultProfile != null && (defaultProfile == newProfile || newProfile.getId().equals(defaultProfile.getId()))) {
            defaultProfile = newProfile;
        }

        List<Profile> newProfileList = new ObjectArrayList<>(state.profiles().size());
        for (Profile profile : state.profiles()) {
            if (newProfile.getId().equals(profile.getId())) {
                newProfileList.add(newProfile);
            } else {
                newProfileList.add(profile);
            }
        }

        return state
                .withProfiles(newProfileList)
                .withSelected(selectedProfile)
                .withDefault(defaultProfile);
    }

    private Reducer() {
    }
}
