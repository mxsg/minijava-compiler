package edu.kit.minijava.backend;

import java.util.*;

import edu.kit.minijava.backend.instructions.*;
import firm.*;
import firm.nodes.*;
import firm.nodes.NodeVisitor.Default;

public class CodeGenerator extends Default {
    private static final String REG_PREFIX = "%@";
    private static final String CONST_PREFIX = "$";
    private static final String REG_WIDTH_D = "d";
    private int currentBlockNr;

    private HashMap<Integer, BasicBlock> intermediateCode = new HashMap<>();

    // Contains the register index the result of each node should be written to
    private HashMap<Node, Integer> node2RegIndex;

    public HashMap<Integer, BasicBlock> getBlockMap() {
        return this.intermediateCode;
    }

    /**
     * If a block with the supplied block number exists, return it. Otherwise, create a new block and insert it
     * into the block map.
     * @param blockNumber The label/number of the block to query or create.
     * @return The block with the queried label (either from the block map or a newly inserted one).
     */
    private BasicBlock getOrCreateBlock(int blockNumber) {
        BasicBlock block = this.intermediateCode.get(blockNumber);

        if (block == null) {
            block = new BasicBlock(blockNumber);
            this.intermediateCode.put(blockNumber, block);
        }

        return block;
    }

    private BasicBlock getCurrentBlock() {
        return this.getOrCreateBlock(this.currentBlockNr);
    }

    private void appendIntermediateInstruction(String instruction) {
        this.appendIntermediateInstruction(instruction, this.currentBlockNr);
    }

    private void appendIntermediateInstruction(String instruction, int blockNr) {
        BasicBlock block = this.getOrCreateBlock(blockNr);
        block.appendInstruction(new GenericInstruction(instruction));
    }

    public CodeGenerator(HashMap<Node, Integer> proj2regIndex) {
        this.node2RegIndex = proj2regIndex;
    }

    public void createValue(int blockNr, Node node) {
        this.currentBlockNr = blockNr;

        switch (node.getOpCode()) {
            case iro_Add:
                Add add = (Add) node;
                this.generate(add);
                break;
            case iro_Sub:
                Sub sub = (Sub) node;
                this.generate(sub);
                break;
            case iro_Mul:
                Mul mul = (Mul) node;
                this.generate(mul);
                break;
            case iro_Div:
                Div div = (Div) node;
                this.generate(div);
                break;
            case iro_Mod:
                Mod mod = (Mod) node;
                this.generate(mod);
                break;
            case iro_Address:
                Address address = (Address) node;
                this.generate(address);
                break;
            case iro_Call:
                Call call = (Call) node;
                this.generate(call);
                break;
            case iro_Cmp:
                Cmp cmp = (Cmp) node;
                this.generate(cmp);
                break;
            case iro_Const:
                Const aConst = (Const) node;
                this.generate(aConst);
                break;
            case iro_Unknown:
                Unknown unknownNode = (Unknown) node;
                this.generate(unknownNode);
                break;
            case iro_End:
                End aEnd = (End) node;
                this.generate(aEnd);
                break;
            case iro_Jmp:
                Jmp aJmp = (Jmp) node;
                this.generate(aJmp);
                break;
            case iro_Load:
                Load aLoad = (Load) node;
                this.generate(aLoad);
                break;
            case iro_Minus:
                Minus aMinus = (Minus) node;
                this.generate(aMinus);
                break;
            case iro_Not:
                Not aNot = (Not) node;
                this.generate(aNot);
                break;
            case iro_Phi:
                Phi aPhi = (Phi) node;
                this.generate(aPhi);
                break;
            case iro_Return:
                Return aReturn = (Return) node;
                this.generate(aReturn);
                break;
            case iro_Sel:
                Sel aSel = (Sel) node;
                this.generate(aSel);
                break;
            case iro_Start:
                Start aStart = (Start) node;
                this.generate(aStart);
                break;
            case iro_Store:
                Store aStore = (Store) node;
                this.generate(aStore);
                break;
            case iro_Proj:
                Proj aProj = (Proj) node;
                this.generate(aProj);
                break;
            case iro_Cond:
                Cond aCond = (Cond) node;
                this.generate(aCond);
                break;
            case iro_Member:
                Member aMember = (Member) node;
                this.generate(aMember);
                break;
            case iro_Conv:
                Conv aConv = (Conv) node;
                this.generate(aConv);
                break;
            default:
                throw new UnsupportedOperationException("unknown node " + node.getClass());
        }
    }

    private void generate(Add add) {
        int srcReg1 = this.node2RegIndex.get(add.getLeft());
        int srcReg2 = this.node2RegIndex.get(add.getRight());
        int targetReg = this.node2RegIndex.get(add);

        this.appendThreeAdressCommand("addl", srcReg1, REG_WIDTH_D, srcReg2, REG_WIDTH_D, targetReg, REG_WIDTH_D);
    }

    private void generate(Address address) {
        // nothing to do
    }

    private void generate(Call call) {

        Address address = (Address) call.getPred(1);
        String functionName = address.getEntity().getLdName();

        // Ignore first two preds, i.e. memory and function address
        int start = 2;

        String args = "";
        String regSuffix = "";

        // build args string
        for (int i = start; i < call.getPredCount(); i++) {
            regSuffix = Util.mode2RegSuffix(call.getPred(i).getMode());

            int arg = this.node2RegIndex.get(call.getPred(i));
            if (call.getPredCount() - i == 1) {
                args += REG_PREFIX + arg + regSuffix;
            }
            else {
                args += REG_PREFIX + arg + regSuffix + " | ";
            }
        }

        boolean isVoid = true;
        for (BackEdges.Edge edge : BackEdges.getOuts(call)) {
            if (edge.node.getMode().equals(Mode.getT())) {
                for (BackEdges.Edge projEdge : BackEdges.getOuts(edge.node)) {
                    regSuffix = Util.mode2RegSuffix(projEdge.node.getMode());
                }
                isVoid = false;
            }
        }

        int targetReg = this.node2RegIndex.get(call);

        if (isVoid) {
            this.appendIntermediateInstruction("call " + functionName
                + " [ " + args + " ] ");
        }
        else {
            this.appendIntermediateInstruction("call " + functionName
                + " [ " + args + " ] -> %@" + targetReg + regSuffix);
        }
    }

    private void generate(Cmp cmp) {
        int srcReg1 = this.node2RegIndex.get(cmp.getLeft());
        int srcReg2 = this.node2RegIndex.get(cmp.getRight());

        String regSuffix = Util.mode2RegSuffix(cmp.getLeft().getMode());
        String cmpSuffix = Util.mode2MovSuffix(cmp.getLeft().getMode());

        String cmd = "cmp" + cmpSuffix;
        String arg1 = REG_PREFIX + srcReg1 + regSuffix;
        String arg2 = REG_PREFIX + srcReg2 + regSuffix;

        BasicBlock block = this.getOrCreateBlock(this.currentBlockNr);

        GenericInstruction compare
            = new GenericInstruction(cmd + " [ " + arg1 + " | " + arg2 + " ]");

        block.setCompare(compare);
    }

    private void generate(Const aConst) {
        if (!aConst.getMode().equals(Mode.getb())) {
            String constant = CONST_PREFIX + String.valueOf(aConst.getTarval().asInt());
            int targetReg = this.node2RegIndex.get(aConst);
            String regSuffix = Util.mode2RegSuffix(aConst.getMode());
            String movSuffix = Util.mode2MovSuffix(aConst.getMode());

            String cmd = "mov" + movSuffix;
            String arg1 = constant + regSuffix;
            String arg2 = REG_PREFIX + targetReg + regSuffix;

            this.appendTwoAdressCommand(cmd, arg1, arg2);
        }
    }

    private void generate(Unknown node) {

        // Assert that we only have Unknown nodes of value types
        Mode nodeMode = node.getMode();
        assert (nodeMode.equals(Mode.getBs()) || nodeMode.equals(Mode.getP())
                || nodeMode.equals(Mode.getIs()) || nodeMode.equals(Mode.getIu()))
                : "Can only handle unknown value nodes.";

        int targetReg = this.node2RegIndex.get(node);

        String constant = CONST_PREFIX + "0";
        String regSuffix = Util.mode2RegSuffix(nodeMode);
        String movSuffix = Util.mode2MovSuffix(nodeMode);

        String cmd = "mov" + movSuffix;
        String arg1 = constant + regSuffix;
        String arg2 = REG_PREFIX + targetReg + regSuffix;

        this.appendTwoAdressCommand(cmd, arg1, arg2);
    }

    private void generate(Div div) {
        int left = this.node2RegIndex.get(div.getLeft());
        int right = this.node2RegIndex.get(div.getRight());

        int targetReg1 = this.node2RegIndex.get(div);
        int targetReg2 = targetReg1 + 1; // by convention used in preparation

        this.appendFourAdressCommand("divl", left, REG_WIDTH_D, right, REG_WIDTH_D, targetReg1, REG_WIDTH_D, targetReg2,
                REG_WIDTH_D);
    }

    private void generate(End end) {
        // nothing to do
    }

    private void generate(Jmp jmp) {
        int numberOfSuccessors = 0;

        for (BackEdges.Edge edge : BackEdges.getOuts(jmp)) {
            // a jmp should never be connected to more than one successor block
            numberOfSuccessors++;
            assert numberOfSuccessors < 2;

            Block block = (Block) edge.node;

            BasicBlock basicBlock = this.getOrCreateBlock(this.currentBlockNr);
            basicBlock.setEndJump(new Jump(this.getOrCreateBlock(block.getNr())));
        }
    }

    private void generate(Load load) {
        int targetReg = this.node2RegIndex.get(load);
        String regSuffix = Util.mode2RegSuffix(load.getLoadMode());
        String movSuffix = Util.mode2MovSuffix(load.getLoadMode());

        if (load.getPred(1) instanceof Sel) {
            Sel sel = (Sel) load.getPred(1);
            int baseReg = this.node2RegIndex.get(sel.getPtr());
            int indexReg = this.node2RegIndex.get(sel.getIndex());
            int alignment = sel.getType().getAlignment();

            this.appendMoveWithOffset(movSuffix, baseReg, indexReg, REG_WIDTH_D, alignment, regSuffix, targetReg,
                    regSuffix);
        }
        else if (load.getPred(1) instanceof Member) {
            Member member = (Member) load.getPred(1);
            int baseReg = this.node2RegIndex.get(member.getPtr());
            int offset = member.getEntity().getOffset();

            this.appendMoveWithOffset(movSuffix, offset, baseReg, targetReg, regSuffix);

        }
        else {
            int pointerReg = this.node2RegIndex.get(load.getPtr());

            this.moveWithOffset(movSuffix, pointerReg, targetReg, regSuffix);
        }
    }

    private void generate(Minus minus) {
        int scrReg = this.node2RegIndex.get(minus.getOp());
        int targetReg = this.node2RegIndex.get(minus);

        this.appendTwoAdressCommand("negl", scrReg, REG_WIDTH_D, targetReg, REG_WIDTH_D);
    }

    private void generate(Mod mod) {
        int srcReg1 = this.node2RegIndex.get(mod.getLeft());
        int srcReg2 = this.node2RegIndex.get(mod.getRight());

        int targetReg1 = this.node2RegIndex.get(mod);
        int targetReg2 = targetReg1 + 1; // by convention used in preparation

        // targetReg2 before targetReg1 is on purpose.
        //Result of mod is second result of divl command and should be stored in the first target register.
        this.appendFourAdressCommand("divl", srcReg1, REG_WIDTH_D, srcReg2, REG_WIDTH_D, targetReg2, REG_WIDTH_D,
                targetReg1, REG_WIDTH_D);
    }

    private void generate(Mul mul) {
        int srcReg1 = this.node2RegIndex.get(mul.getLeft());
        int srcReg2 = this.node2RegIndex.get(mul.getRight());

        int targetReg = this.node2RegIndex.get(mul);

        this.appendThreeAdressCommand("mull", srcReg1, REG_WIDTH_D, srcReg2, REG_WIDTH_D, targetReg, REG_WIDTH_D);
    }

    private void generate(Not not) {
        int srcReg = this.node2RegIndex.get(not.getOp());
        int targetReg = this.node2RegIndex.get(not);

        this.appendTwoAdressCommand("notb", srcReg, "l", targetReg, "l");
    }

    private void generate(Return aReturn) {

        if (aReturn.getPredCount() == 1 && !aReturn.getPred(0).getMode().equals(Mode.getM())) {
            String regSuffix = Util.mode2RegSuffix(aReturn.getPred(0).getMode());
            String movSuffix = Util.mode2MovSuffix(aReturn.getPred(0).getMode());

            String cmd = "mov" + movSuffix;
            String src = REG_PREFIX + this.node2RegIndex.get(aReturn.getPred(0));
            src += regSuffix;
            String target = "%@$" + regSuffix;

            this.appendTwoAdressCommand(cmd, src, target);
        }
        else if (aReturn.getPredCount() > 1) {
            String regSuffix = Util.mode2RegSuffix(aReturn.getPred(1).getMode());
            String movSuffix = Util.mode2MovSuffix(aReturn.getPred(1).getMode());

            String cmd = "mov" + movSuffix;
            String src = REG_PREFIX + this.node2RegIndex.get(aReturn.getPred(1));
            src += regSuffix;
            String target = "%@$" + regSuffix;

            this.appendTwoAdressCommand(cmd, src, target);
        }

        // Select single successor
        Block successorBlock = (Block) BackEdges.getOuts(aReturn).iterator().next().node;

        this.getCurrentBlock().setEndJump(new Jump(this.getOrCreateBlock(successorBlock.getNr())));
    }

    private void generate(Sel sel) {
    }

    private void generate(Start start) {
        // nothing to do
    }

    private void generate(Store store) {
        int storeReg = this.node2RegIndex.get(store.getValue());
        String regSuffix = Util.mode2RegSuffix(store.getValue().getMode());
        String movSuffix = Util.mode2MovSuffix(store.getValue().getMode());

        if (store.getPred(1) instanceof Sel) {
            Sel sel = (Sel) store.getPred(1);
            int baseReg = this.node2RegIndex.get(sel.getPtr());
            int indexReg = this.node2RegIndex.get(sel.getIndex());
            int alignment = sel.getType().getAlignment();

            this.appendStoreCmd(movSuffix, regSuffix, storeReg, baseReg, indexReg, alignment);
        }
        else if (store.getPred(1) instanceof Member) {
            Member member = (Member) store.getPred(1);
            int baseReg = this.node2RegIndex.get(member.getPtr());
            int offset = member.getEntity().getOffset();

            this.appendStoreCmd(movSuffix, storeReg, regSuffix, offset, baseReg);
        }
        else {
            int pointerReg = this.node2RegIndex.get(store.getPtr());
            this.appendStoreCmd(movSuffix, storeReg, regSuffix, pointerReg);
        }
    }

    private void generate(Sub sub) {
        int srcReg1 = this.node2RegIndex.get(sub.getLeft());
        int srcReg2 = this.node2RegIndex.get(sub.getRight());

        int targetReg = this.node2RegIndex.get(sub);

        String regSuffix = Util.mode2RegSuffix(sub.getMode());

        this.appendThreeAdressCommand("subl", srcReg1, regSuffix, srcReg2, regSuffix, targetReg, regSuffix);

    }

    private void generate(Proj node) {
        // nothing to do
    }

    private void generate(Phi phi) {

        if (phi.getMode().equals(Mode.getM())) {

            // Ignore Phi nodes with memory mode as these are only important for ordering nodes
            return;
        }

        String registerSuffix = Util.mode2RegSuffix(phi.getMode());
        String moveSuffix = Util.mode2MovSuffix(phi.getMode());

        int phiTargetRegister = this.node2RegIndex.get(phi);

        List<PhiNode.Mapping> phiMappings = new ArrayList<>();

        for (int i = 0; i < phi.getPredCount(); i++) {
            int predRegisterIndex = this.node2RegIndex.get(phi.getPred(i));
            int predBlockLabel = phi.getBlock().getPred(i).getBlock().getNr();

            this.intermediateCode.putIfAbsent(predBlockLabel, new BasicBlock(predBlockLabel));

            BasicBlock predBlock = this.intermediateCode.get(predBlockLabel);

            // Add the entry into the Phi node representation
            phiMappings.add(new PhiNode.Mapping(predBlock, predRegisterIndex, phiTargetRegister,
                registerSuffix, moveSuffix));
        }

        // Insert a new PhiNode representation into the current basic block
        BasicBlock currentBlock = this.getOrCreateBlock(this.currentBlockNr);

        PhiNode phiNode = new PhiNode(currentBlock, phiTargetRegister, phiMappings);
        currentBlock.addPhiNode(phiNode);
    }

    private void generate(Cond cond) {
        Node selector = cond.getSelector();

        ConditionalJump condJump = null;
        Jump uncondJump = null;

        for (BackEdges.Edge edge : BackEdges.getOuts(cond)) {

            // Cond nodes should always be succeeded by proj nodes (with X mode set)
            Proj proj = (Proj) edge.node;

            // Get block which is the successor of the proj node
            // iterator().next() always yields the first entry in the iteration
            Block block = (Block) BackEdges.getOuts(proj).iterator().next().node;

            BasicBlock target = this.getOrCreateBlock(block.getNr());

            if (selector instanceof Cmp) {

                Cmp cmp = (Cmp) selector;
                Relation relation = cmp.getRelation();

                // Only generate a conditional jump for the true part, otherwise generate an unconditional jump
                if (proj.getNum() == Cond.pnTrue) {

                    String mnemonic = Util.relation2Jmp(relation);
                    condJump = new ConditionalJump(mnemonic, target);
                }
                else {
                    uncondJump = new Jump(target);
                }
            }
            else {
                // The input for the Cond node is a constant, which may result from constant folding.
                // In this case, generate constant compare instructions.

                assert selector instanceof Const && selector.getMode().equals(Mode.getb());
                Const aConst = (Const) selector;

                if (proj.getNum() == Cond.pnTrue && aConst.getTarval().equals(TargetValue.getBTrue())) {

                    BasicBlock currentBlock = this.getOrCreateBlock(this.currentBlockNr);
                    GenericInstruction compare
                        = new GenericInstruction("cmpl" + " [ $-1d | $0d ]");
                    currentBlock.setCompare(compare);

                    condJump = new ConditionalJump("jl", target);
                    uncondJump = new Jump(target);

                }
                else if (proj.getNum() == Cond.pnFalse && aConst.getTarval().equals(TargetValue.getBFalse())) {

                    BasicBlock currentBlock = this.getOrCreateBlock(this.currentBlockNr);
                    GenericInstruction compare
                        = new GenericInstruction("cmpl" + " [ $-1d | $0d ]");
                    currentBlock.setCompare(compare);

                    condJump = new ConditionalJump("jg", target);
                    uncondJump = new Jump(target);
                }
            }
        }

        BasicBlock currentBlock = this.getCurrentBlock();

        if (condJump != null) {
            currentBlock.setConditionalJump(condJump);
        }
        if (uncondJump != null) {
            currentBlock.setEndJump(uncondJump);
        }
    }

    private void generate(Member node) {
        // nothing to do
    }

    private void generate(Conv node) {
        // Nothing to do here as conversion nodes are only constructed for div and mod
        // operations and conversion of the operands instead should be handled there.
    }


    /**
     * <p>
     * Example<br>
     * <br>
     * negl %@17d -> %@18d
     * </p>
     *
     * @param cmd name of command
     * @param srcReg number of source register to use
     * @param suffixSrcReg width of source register, e.g. 'd'
     * @param targetReg number of target register to use
     * @param suffixTargetReg width of target register, e.g. 'd'
     */
    private void appendTwoAdressCommand(String cmd, int srcReg, String suffixSrcReg, int targetReg,
            String suffixTargetReg) {
        this.appendIntermediateInstruction(
                cmd + " " + REG_PREFIX + srcReg + suffixSrcReg + " -> " + REG_PREFIX + targetReg + suffixTargetReg);
    }

    /**
     * append cmd src -> target
     * <p>
     * Example<br>
     * <br>
     * mov %@8 -> %@$
     * </p>
     *
     * @param cmd name of command
     * @param src String to use as source, will be included as it is, i.e. REG_PREFIX will not be appended
     * @param target String to use as target, will be included as it is, i.e REG_PREFIX will not be append
     */
    private void appendTwoAdressCommand(String cmd, String src, String target) {
        this.appendIntermediateInstruction(cmd + " " + src + " -> " + target);
    }

    /**
     * append command cmd [ srcReg1 | srcReg2 ] -> targetReg
     *
     * <p>
     * Example<br>
     * <br>
     * add [ %@21d | %@22d ] -> %@23d
     * </p>
     *
     * @param cmd             command
     * @param srcReg1         number of first source register
     * @param suffixReg1      width of first register
     * @param srcReg2         number of second source register
     * @param suffixReg2      width of second register
     * @param targetReg       number of target register
     * @param suffixTargetReg width of target register
     */
    private void appendThreeAdressCommand(String cmd, int srcReg1, String suffixReg1, int srcReg2, String suffixReg2,
            int targetReg, String suffixTargetReg) {
        this.appendIntermediateInstruction(cmd
                + " [ " + REG_PREFIX + srcReg1 + suffixReg1
                + " | " + REG_PREFIX + srcReg2 + suffixReg2 + " ] "
                + "-> " + REG_PREFIX + targetReg + suffixTargetReg);
    }

    /**
     * append command with two source registers and two target registers
     * <p>
     * Example<br>
     * <br>
     * <code>divl [ %@21d | %@22d ] -> [ %@23d | %@24d ]</code>
     * </p>
     *
     * @param cmd              command
     * @param srcReg1          number of first source register to use
     * @param suffixReg1       width of srcReg1, e.g. 'd'
     * @param srcReg2          number of second source register to use
     * @param suffixReg2       width of srcReg2, e.g. 'd'
     * @param targetReg1       number of first target register to use
     * @param suffixTargetReg1 width of targetReg1, e.g. 'd'
     * @param targetReg2       number of second target register to use
     * @param suffixTargetReg2 width of targetReg2, e.g. 'd'
     */
    private void appendFourAdressCommand(String cmd, int srcReg1, String suffixReg1, int srcReg2, String suffixReg2,
            int targetReg1, String suffixTargetReg1, int targetReg2, String suffixTargetReg2) {

        this.appendIntermediateInstruction(cmd + " [ " + REG_PREFIX + srcReg1 + suffixReg1
                + " | " + REG_PREFIX + srcReg2 + suffixReg2
                + " ] -> [ " + REG_PREFIX + targetReg1 + suffixTargetReg1
                + " | " + REG_PREFIX + targetReg2 + suffixTargetReg2 + " ]");
    }

    /**
     * <p>
     * Example<br>
     * <br>
     * movd (%@17, %@18d, 10) -> %@19
     * </p>
     *
     * @param moveSuffix suffix to append to 'mov' command, e.g. 'd'
     * @param baseReg number of base register
     * @param indexReg number of index register
     * @param suffixIndexReg width of index register, e.g. 'd'
     * @param alignment alignment of object, used to compute offset
     * @param suffixSrcReg width of source register, e.g. 'd'
     * @param targetReg number of target register
     * @param suffixTargetReg width of target register,e.g 'd'
     */
    private void appendMoveWithOffset(String moveSuffix, int baseReg, int indexReg, String suffixIndexReg,
            int alignment, String suffixSrcReg, int targetReg, String suffixTargetReg) {

        StringBuilder sb = new StringBuilder("mov");
        sb.append(moveSuffix).append(" ( ");
        sb.append(REG_PREFIX).append(baseReg);
        sb.append(", ").append(REG_PREFIX).append(indexReg).append(suffixIndexReg);
        sb.append(", ").append(alignment).append(")").append(suffixSrcReg);
        sb.append(" -> ").append(REG_PREFIX).append(targetReg).append(suffixTargetReg);

        this.appendIntermediateInstruction(sb.toString());
    }

    /**
     * <p>
     * Example<br>
     * <br>
     * movd 7(%@19)d -> %@20d
     * </p>
     *
     * @param movSuffix suffix to append to 'mov' command, e.g. 'd'
     * @param offset offset added to base
     * @param baseReg number of base register
     * @param targetReg number of target register
     * @param suffixTargetReg width of register, e.g. 'd'
     */
    private void appendMoveWithOffset(String movSuffix, int offset, int baseReg, int targetReg,
            String suffixTargetReg) {
        this.appendIntermediateInstruction("mov" + movSuffix + " "
                + offset + "(" + REG_PREFIX + baseReg + ")" + suffixTargetReg
                + " -> " + REG_PREFIX + targetReg + suffixTargetReg);
    }

    /**
     * <p>
     * Example<br>
     * <br>
     * movd (%@17) -> %@18d
     * </p>
     *
     * @param movSuffix suffix to append to 'mov' command, e.g. 'd'
     * @param pointerReg number of register containing pointer
     * @param targetReg number of target register
     * @param suffixTargetReg width of target register, e.g. 'd'
     */
    private void moveWithOffset(String movSuffix, int pointerReg, int targetReg, String suffixTargetReg) {
        this.appendIntermediateInstruction("mov" + movSuffix + " "
                + "(" + REG_PREFIX + pointerReg + ") -> " + REG_PREFIX + targetReg + suffixTargetReg);
    }

    /**
     * <p>
     * Exmaple<br>
     * <br>
     * movq %@17d -> 8(%@18)d
     * </p>
     *
     * @param movSuffix suffix to append to 'mov' command, e.g 'd'
     * @param storeReg number of register to store
     * @param regSuffix width of registers, e.g. 'd'
     * @param offset offset to add to base
     * @param baseReg number of base register
     */
    private void appendStoreCmd(String movSuffix, int storeReg, String regSuffix, int offset, int baseReg) {
        this.appendIntermediateInstruction("mov" + movSuffix + " "
                + REG_PREFIX + storeReg + regSuffix + " -> " + offset + "("
                + REG_PREFIX + baseReg + ")" + regSuffix);
    }

    /**
     * <p>
     * Example<br>
     * <br>
     * movq %@17d -> (%@18, %@19d, 10)d
     * </p>
     *
     * @param movSuffix suffix to add to 'mov' command, e.g. 'd'
     * @param regSuffix width of registers, e.g. 'd'
     * @param storeReg number of register to store
     * @param baseReg number of base register
     * @param indexReg number of index register
     * @param alignment alignment of object, used to compute offset
     */
    private void appendStoreCmd(String movSuffix, String regSuffix, int storeReg, int baseReg, int indexReg,
            int alignment) {
        this.appendIntermediateInstruction("mov" + movSuffix + " "
                + REG_PREFIX + storeReg + regSuffix + " -> (" + REG_PREFIX
                + baseReg + ", " + REG_PREFIX + indexReg + REG_WIDTH_D + ", " + alignment + ")" + regSuffix);
    }

    /**
     * Example<br>
     * <br>
     * movq %@17d -> (%@18)d
     *
     * @param movSuffix suffix to append to 'mov' command
     * @param storeReg number of register to store
     * @param regSuffix width of register, e.g. 'd'
     * @param pointerReg number of register containing pointer
     */
    private void appendStoreCmd(String movSuffix, int storeReg, String regSuffix, int pointerReg) {
        this.appendIntermediateInstruction("mov" + movSuffix + " "
                + REG_PREFIX + storeReg + regSuffix + " -> (" + REG_PREFIX
                + pointerReg + ")" + regSuffix);
    }
}
