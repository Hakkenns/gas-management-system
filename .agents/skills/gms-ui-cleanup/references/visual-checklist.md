# Visual checklist

- Confirm the view keeps the AdminLTE layout and existing asset loading order.
- Prefer `card-header`/`card-body`, Bootstrap spacing utilities and semantic button variants.
- Wrap wide tables in `table-responsive`; check headers, actions and empty states.
- Check modal size, centered layout, footer actions, backdrop and keyboard dismissal.
- Check forms for label alignment, control widths, validation states and mobile stacking.
- Check active sidebar state and visual hierarchy after partial navigation.
- Avoid fixed widths that break narrow viewports and avoid global selectors with unrelated impact.
- Manually inspect desktop and a narrow viewport when the change affects layout.

## Sidebar reflow for wide content

For views with tables, cards, filters or other wide content, validate this sequence:

1. Sidebar expanded.
2. Sidebar collapsed.
3. Sidebar expanded again.

At each transition, confirm that:

- content reflows into the available width;
- tables use the available width correctly;
- table headers and body columns remain aligned;
- actions remain visible;
- DataTables remains functional;
- responsive behavior remains correct;
- no unnecessary fixed width causes clipping;
- no stale width leaves unexpected empty space;
- no unnecessary horizontal overflow appears.

If DataTables needs dimension recalculation after the sidebar changes, use the smallest solution compatible with the module. Do not impose a concrete API such as `columns.adjust()` in every module. Do not add duplicate global listeners; if JavaScript is needed, respect the module lifecycle and `AppModules` contract.
