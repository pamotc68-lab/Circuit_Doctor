package com.example.circuitdoctor;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BreadboardSimulatorActivity extends AppCompatActivity {

    public static final String EXTRA_CIRCUIT_ID = "extra_circuit_id";
    public static final String EXTRA_CHALLENGE_ID = "extra_challenge_id";

    private ImageView btnBreadboardBack;
    private ImageView btnUndo;
    private ImageView btnRedo;
    private ImageView btnDeleteComponent;
    private ImageView btnWireTool;
    private TextView btnSimulate;
    private TextView btnSaveCircuit;
    private TextView btnSendTo;
    private EditText etComponentSearch;
    private RecyclerView recyclerComponentPalette;
    private CircuitCanvasView circuitCanvasView;

    private CircuitDesignModel model;
    private ComponentCatalog catalog;
    private ComponentPaletteAdapter paletteAdapter;
    private CircuitStorageRepository storageRepository;

    private final Deque<CircuitState> undoStack = new ArrayDeque<>();
    private final Deque<CircuitState> redoStack = new ArrayDeque<>();

    private Long selectedComponentId = null;
    private Long loadedCircuitId = null;
    private CircuitChallenge activeChallenge = null;
    private boolean simulating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breadboard_simulator);
        new UsageLogRepository(this).logVisit("ชื่อหน้าที่จะแสดงในประวัติ");

        bindViews();
        model = new CircuitDesignModel();
        catalog = new ComponentCatalog();
        storageRepository = new CircuitStorageRepository(this);

        setupCanvas();
        setupPalette();
        setupClickListeners();
        loadFromHistoryIfNeeded();
        loadChallengeIfNeeded();
        updateToolbarState();
    }

    private void bindViews() {
        btnBreadboardBack = findViewById(R.id.btnBreadboardBack);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnDeleteComponent = findViewById(R.id.btnDeleteComponent);
        btnWireTool = findViewById(R.id.btnWireTool);
        btnSimulate = findViewById(R.id.btnSimulate);
        btnSaveCircuit = findViewById(R.id.btnSaveCircuit);
        btnSendTo = findViewById(R.id.btnSendTo);
        etComponentSearch = findViewById(R.id.etComponentSearch);
        recyclerComponentPalette = findViewById(R.id.recyclerComponentPalette);
        circuitCanvasView = findViewById(R.id.circuitCanvasView);
    }

    private void setupCanvas() {
        circuitCanvasView.setModel(model);
        circuitCanvasView.setListener(new CircuitCanvasView.Listener() {
            @Override
            public void onComponentSelected(Long componentId) {
                selectedComponentId = componentId;
                updateToolbarState();
            }

            @Override
            public void onBeforeChange() {
                pushUndo();
            }

            @Override
            public void onCircuitChanged() {
                redoStack.clear();
                if (simulating) {
                    simulating = false;
                    circuitCanvasView.clearSimulationResult();
                }
                updateToolbarState();
            }

            @Override
            public void onPlacementTypeConsumed() {
                paletteAdapter.setSelectedTypeId(-1);
            }

            @Override
            public void onEditPropertiesRequested(long componentId, int typeId) {
                if (typeId == ComponentType.RESISTOR) {
                    showResistorDialog(componentId);
                } else if (typeId == ComponentType.BATTERY_9V) {
                    showBatteryDialog(componentId);
                }
            }
        });
    }

    private void setupPalette() {
        recyclerComponentPalette.setLayoutManager(new LinearLayoutManager(this));
        paletteAdapter = new ComponentPaletteAdapter(catalog.getComponents(this), type -> {
            circuitCanvasView.setPendingPlacementType(type.id);
            paletteAdapter.setSelectedTypeId(type.id);
            selectedComponentId = null;
            Toast.makeText(this, R.string.circuit_tap_board_to_place, Toast.LENGTH_SHORT).show();
            updateToolbarState();
        });
        recyclerComponentPalette.setAdapter(paletteAdapter);

        etComponentSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                paletteAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadFromHistoryIfNeeded() {
        if (!getIntent().hasExtra(EXTRA_CIRCUIT_ID)) return;
        loadedCircuitId = getIntent().getLongExtra(EXTRA_CIRCUIT_ID, -1);
        CircuitState state = storageRepository.loadById(loadedCircuitId);
        model.restore(state);
        circuitCanvasView.refresh();
    }

    private void loadChallengeIfNeeded() {
        if (!getIntent().hasExtra(EXTRA_CHALLENGE_ID)) return;
        int challengeId = getIntent().getIntExtra(EXTRA_CHALLENGE_ID, -1);

        ChallengeRepository repo = new ChallengeRepository();
        for (CircuitChallenge c : repo.getChallenges()) {
            if (c.id == challengeId) {
                activeChallenge = c;
                break;
            }
        }
        if (activeChallenge == null) return;

        btnSendTo.setText(R.string.challenge_check_button);
        new AlertDialog.Builder(this)
                .setTitle(activeChallenge.title)
                .setMessage(activeChallenge.conditionText)
                .setPositiveButton(R.string.circuit_save_button_confirm, null)
                .show();
    }

    private void setupClickListeners() {
        btnBreadboardBack.setOnClickListener(v -> finish());

        btnUndo.setOnClickListener(v -> handleUndo());
        btnRedo.setOnClickListener(v -> handleRedo());
        btnDeleteComponent.setOnClickListener(v -> handleDelete());
        btnWireTool.setOnClickListener(v -> handleCancelSelection());

        btnSimulate.setOnClickListener(v -> toggleSimulation());
        btnSaveCircuit.setOnClickListener(v -> handleSave());
        btnSendTo.setOnClickListener(v -> handleSend());
    }

    private void handleCancelSelection() {
        circuitCanvasView.clearSelection();
        paletteAdapter.setSelectedTypeId(-1);
        selectedComponentId = null;
        updateToolbarState();
    }

    private void showResistorDialog(long componentId) {
        PlacedComponent pc = model.findComponentById(componentId);
        if (pc == null) return;
        double current = pc.customOhms > 0 ? pc.customOhms : 220.0;

        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(current == Math.rint(current) ? String.valueOf((long) current) : String.valueOf(current));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.circuit_edit_resistor_title)
                .setMessage(R.string.circuit_edit_resistor_hint)
                .setView(input)
                .setPositiveButton(R.string.circuit_save_button_confirm, (dialog, which) -> {
                    double ohms = parsePositiveDouble(input.getText().toString());
                    if (ohms <= 0) {
                        Toast.makeText(this, R.string.circuit_edit_value_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    applyPropertyChange(() -> model.setResistorOhms(componentId, ohms));
                })
                .setNegativeButton(R.string.circuit_save_button_cancel, null)
                .show();
    }

    private void showBatteryDialog(long componentId) {
        PlacedComponent pc = model.findComponentById(componentId);
        if (pc == null) return;
        double current = pc.customVoltage > 0 ? pc.customVoltage : 9.0;

        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(current == Math.rint(current) ? String.valueOf((long) current) : String.valueOf(current));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.circuit_edit_battery_title)
                .setMessage(R.string.circuit_edit_battery_hint)
                .setView(input)
                .setPositiveButton(R.string.circuit_save_button_confirm, (dialog, which) -> {
                    double volts = parsePositiveDouble(input.getText().toString());
                    if (volts <= 0) {
                        Toast.makeText(this, R.string.circuit_edit_value_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    applyPropertyChange(() -> model.setBatteryVoltage(componentId, volts));
                })
                .setNegativeButton(R.string.circuit_save_button_cancel, null)
                .show();
    }

    private double parsePositiveDouble(String text) {
        try {
            double v = Double.parseDouble(text.trim());
            return v > 0 ? v : -1;
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }

    private void applyPropertyChange(Runnable change) {
        pushUndo();
        change.run();
        redoStack.clear();
        simulating = false;
        circuitCanvasView.clearSimulationResult();
        circuitCanvasView.refresh();
        updateToolbarState();
    }

    private void pushUndo() {
        undoStack.push(model.snapshot());
        if (undoStack.size() > 30) {
            undoStack.removeLast();
        }
    }

    private void handleUndo() {
        if (undoStack.isEmpty()) return;
        simulating = false;
        circuitCanvasView.clearSimulationResult();
        redoStack.push(model.snapshot());
        CircuitState prev = undoStack.pop();
        model.restore(prev);
        circuitCanvasView.clearSelection();
        circuitCanvasView.refresh();
        selectedComponentId = null;
        paletteAdapter.setSelectedTypeId(-1);
        updateToolbarState();
    }

    private void handleRedo() {
        if (redoStack.isEmpty()) return;
        simulating = false;
        circuitCanvasView.clearSimulationResult();
        undoStack.push(model.snapshot());
        CircuitState next = redoStack.pop();
        model.restore(next);
        circuitCanvasView.clearSelection();
        circuitCanvasView.refresh();
        selectedComponentId = null;
        paletteAdapter.setSelectedTypeId(-1);
        updateToolbarState();
    }

    private void handleDelete() {
        if (selectedComponentId == null) {
            Toast.makeText(this, R.string.circuit_select_component_first, Toast.LENGTH_SHORT).show();
            return;
        }
        simulating = false;
        circuitCanvasView.clearSimulationResult();
        pushUndo();
        model.removeComponent(selectedComponentId);
        redoStack.clear();
        selectedComponentId = null;
        circuitCanvasView.clearSelection();
        circuitCanvasView.refresh();
        updateToolbarState();
    }

    private void toggleSimulation() {
        simulating = !simulating;
        if (simulating) {
            SimulationResult result = model.solve();
            circuitCanvasView.showSimulationResult(result);
            showResultMessage(result);
        } else {
            circuitCanvasView.clearSimulationResult();
        }
        updateToolbarState();
    }

    private void showResultMessage(SimulationResult result) {
        String message;
        if (!result.hasBattery) {
            message = getString(R.string.circuit_sim_no_battery);
        } else if (!result.pathFound) {
            message = getString(R.string.circuit_sim_incomplete);
        } else if (result.overCurrentDanger) {
            message = getString(R.string.circuit_sim_overcurrent);
        } else if (result.hasLed && !result.isLedLit()) {
            message = getString(R.string.circuit_sim_led_off_low_voltage);
        } else {
            String detail = getString(
                    R.string.circuit_sim_success_detail,
                    String.format("%.1f", result.currentAmps * 1000f),
                    (int) result.totalResistanceOhms
            );
            message = getString(R.string.circuit_sim_success) + "\n" + detail;
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleSave() {
        if (loadedCircuitId != null) {
            storageRepository.updateExisting(loadedCircuitId, model.getComponents(), model.getConnections());
            Toast.makeText(this, R.string.circuit_update_success, Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setHint(R.string.circuit_save_dialog_hint);

        new AlertDialog.Builder(this)
                .setTitle(R.string.circuit_save_dialog_title)
                .setView(input)
                .setPositiveButton(R.string.circuit_save_button_confirm, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = getString(R.string.circuit_default_name);
                    loadedCircuitId = storageRepository.saveNew(name, model.getComponents(), model.getConnections());
                    Toast.makeText(this, R.string.circuit_save_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.circuit_save_button_cancel, null)
                .show();
    }

    private void handleSend() {
        if (activeChallenge != null) {
            checkChallengeAnswer();
            return;
        }
        Toast.makeText(this, R.string.circuit_send_placeholder, Toast.LENGTH_SHORT).show();
    }

    private void checkChallengeAnswer() {
        Map<Integer, Integer> placedCounts = countByType(model.getComponents());
        Map<Integer, Integer> requiredCounts = new HashMap<>();
        for (int typeId : activeChallenge.requiredTypeIds) {
            Integer current = requiredCounts.get(typeId);
            requiredCounts.put(typeId, current == null ? 1 : current + 1);
        }

        for (Map.Entry<Integer, Integer> entry : requiredCounts.entrySet()) {
            Integer have = placedCounts.get(entry.getKey());
            int haveCount = have == null ? 0 : have;
            if (haveCount < entry.getValue()) {
                Toast.makeText(this, R.string.challenge_missing_components, Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (activeChallenge.requireWorkingCircuit) {
            SimulationResult result = model.solve();
            boolean works = result.hasBattery && result.pathFound && !result.overCurrentDanger;
            if (!works) {
                Toast.makeText(this, R.string.challenge_circuit_not_working, Toast.LENGTH_LONG).show();
                return;
            }
        }

        Toast.makeText(this, R.string.challenge_success, Toast.LENGTH_LONG).show();
    }

    private Map<Integer, Integer> countByType(List<PlacedComponent> components) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (PlacedComponent p : components) {
            Integer current = counts.get(p.typeId);
            counts.put(p.typeId, current == null ? 1 : current + 1);
        }
        return counts;
    }

    private void updateToolbarState() {
        btnUndo.setEnabled(!undoStack.isEmpty());
        btnUndo.setAlpha(undoStack.isEmpty() ? 0.4f : 1f);

        btnRedo.setEnabled(!redoStack.isEmpty());
        btnRedo.setAlpha(redoStack.isEmpty() ? 0.4f : 1f);

        btnDeleteComponent.setEnabled(selectedComponentId != null);
        btnDeleteComponent.setAlpha(selectedComponentId != null ? 1f : 0.4f);

        btnSimulate.setText(simulating ? R.string.circuit_button_stop_sim : R.string.circuit_button_start_sim);
    }
}