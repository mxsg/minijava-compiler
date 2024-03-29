//
//  Instructions.swift
//  Molki
//
//  Created by Christian Schnorr on 14.01.19.
//  Copyright © 2019 Christian Schnorr. All rights reserved.
//

import Swift



public protocol InstructionProtocol: CustomStringConvertible {
    func apply(_ closure: (inout Argument<Pseudoregister>) -> Void)
    func apply(_ closure: (inout Result<Pseudoregister>) -> Void)

    var hasEffectsOtherThanWritingPseudoregisters: Bool { get }
}

extension InstructionProtocol {
    public var arguments: [Argument<Pseudoregister>] {
        var arguments: [Argument<Pseudoregister>] = []

        self.apply({ argument in
            arguments.append(argument)
        })

        return arguments
    }

    public var results: [Result<Pseudoregister>] {
        var results: [Result<Pseudoregister>] = []

        self.apply({ result in
            results.append(result)
        })

        return results
    }

    public var pseudoregisters: (read: Set<Pseudoregister>, written: Set<Pseudoregister>) {
        var read: Set<Pseudoregister> = []
        var written: Set<Pseudoregister> = []

        for argument in self.arguments {
            switch argument {
            case .constant:
                break
            case .register(let value):
                read.insert(value.register)
            case .memory(let value):
                read.formUnion(value.registers)
            }
        }

        for result in self.results {
            switch result {
            case .register(let value):
                written.insert(value.register)
            case .memory(let value):
                read.formUnion(value.registers)
            }
        }

        return (read: read, written: written)
    }

    public func substitute(_ pseudoregister: Pseudoregister, with replacement: Argument<Pseudoregister>) {
        self.apply({ (argument: inout Argument<Pseudoregister>) in
            argument.substitute(pseudoregister, with: replacement)
        })

        self.apply({ (result: inout Result<Pseudoregister>) in
            result.substitute(pseudoregister, with: replacement)
        })
    }
}

public enum Instruction: InstructionProtocol {
    case labelInstruction(LabelInstruction)
    case jumpInstruction(JumpInstruction)
    case callInstruction(CallInstruction)
    case moveInstruction(MoveInstruction)
    case comparisonInstruction(ComparisonInstruction)
    case additionInstruction(AdditionInstruction)
    case subtractionInstruction(SubtractionInstruction)
    case multiplicationInstruction(MultiplicationInstruction)
    case divisionInstruction(DivisionInstruction)
    case numericNegationInstruction(NumericNegationInstruction)
    case logicalNegationInstruction(LogicalNegationInstruction)

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
        self.rawInstruction.apply(closure)
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
        self.rawInstruction.apply(closure)
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return self.rawInstruction.hasEffectsOtherThanWritingPseudoregisters
    }

    public var description: String {
        return self.rawInstruction.description
    }

    public var rawInstruction: AnyObject & InstructionProtocol {
        switch self {
        case .labelInstruction(let instruction):
            return instruction
        case .jumpInstruction(let instruction):
            return instruction
        case .callInstruction(let instruction):
            return instruction
        case .moveInstruction(let instruction):
            return instruction
        case .comparisonInstruction(let instruction):
            return instruction
        case .additionInstruction(let instruction):
            return instruction
        case .subtractionInstruction(let instruction):
            return instruction
        case .multiplicationInstruction(let instruction):
            return instruction
        case .divisionInstruction(let instruction):
            return instruction
        case .logicalNegationInstruction(let instruction):
            return instruction
        case .numericNegationInstruction(let instruction):
            return instruction
        }
    }
}

extension Instruction: Equatable, Hashable {
    public static func == (lhs: Instruction, rhs: Instruction) -> Bool {
        return lhs.rawInstruction === rhs.rawInstruction
    }

    public func hash(into hasher: inout Hasher) {
        ObjectIdentifier(self.rawInstruction).hash(into: &hasher)
    }
}




public class LabelInstruction: InstructionProtocol {
    public init(name: String) {
        self.name = name
    }

    private(set) public var name: String

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return true // can be used as jump target
    }

    public var description: String {
        return "\(self.name):"
    }
}

// jmp, jl, jle, jg, jge, je, jne
public class JumpInstruction: InstructionProtocol {
    public init(target: String, condition: JumpCondition) {
        self.target = target
        self.condition = condition
    }

    private(set) public var target: String
    private(set) public var condition: JumpCondition

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return true
    }

    public var description: String {
        return "\(self.condition.rawValue) \(self.target)"
    }
}

public enum JumpCondition: String {
    case unconditional = "jmp"
    case lessThan = "jl"
    case lessThanOrEqualTo = "jle"
    case greaterThan = "jg"
    case greaterThanOrEqualTo = "jge"
    case equalTo = "je"
    case notEqualTo = "jne"
}

// call
// Syntax: call f [ a | b | c | ... ] -> z
// Semantic: z = f(a, b, c, ...)
public class CallInstruction: InstructionProtocol {
    public init(target: String, arguments: [Argument<Pseudoregister>], result: Result<Pseudoregister>?) {
        self.target = target
        self.arguments = arguments
        self.result = result
    }

    private(set) public var target: String
    private(set) public var arguments: [Argument<Pseudoregister>]
    private(set) public var result: Result<Pseudoregister>?

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
        for index in self.arguments.indices {
            closure(&self.arguments[index])
        }
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
        if self.result != nil {
            closure(&self.result!)
        }
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return true
    }

    public var description: String {
        var description = "call \(self.target)"

        if !self.arguments.isEmpty {
            description += " [ "
            description += self.arguments.map({ "\($0)" }).joined(separator: " | ")
            description += " ]"
        }

        if let result = self.result {
            description += " -> \(result)"
        }

        return description
    }
}

// mov
// Syntax: movx a -> b
// Semantic: b := a
public class MoveInstruction: InstructionProtocol {
    public init(width: RegisterWidth, source: Argument<Pseudoregister>, target: Result<Pseudoregister>) {
        self.width = width
        self.source = source
        self.target = target
    }

    private(set) public var width: RegisterWidth
    private(set) public var source: Argument<Pseudoregister>
    private(set) public var target: Result<Pseudoregister>

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
        closure(&self.source)
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
        closure(&self.target)
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        switch self.target {
        case .register:
            return false
        case .memory:
            return true
        }
    }

    public var description: String {
        switch self.width {
        case .byte: return "movb \(self.source) -> \(self.target)"
        case .word: return "movw \(self.source) -> \(self.target)"
        case .double: return "movl \(self.source) -> \(self.target)"
        case .quad: return "movq \(self.source) -> \(self.target)"
        }
    }
}

// Synax: cmpx [ a | b ]
// Semantic: flags := a ? b
public class ComparisonInstruction: InstructionProtocol {
    public init(width: RegisterWidth, lhs: Argument<Pseudoregister>, rhs: Argument<Pseudoregister>) {
        self.width = width
        self.lhs = lhs
        self.rhs = rhs
    }

    private(set) public var width: RegisterWidth
    private(set) public var lhs: Argument<Pseudoregister>
    private(set) public var rhs: Argument<Pseudoregister>

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
        closure(&self.lhs)
        closure(&self.rhs)
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return true
    }

    public var description: String {
        switch self.width {
        case .byte: return "cmpb [ \(self.lhs) | \(self.rhs) ]"
        case .word: return "cmpw [ \(self.lhs) | \(self.rhs) ]"
        case .double: return "cmpl [ \(self.lhs) | \(self.rhs) ]"
        case .quad: return "cmpq [ \(self.lhs) | \(self.rhs) ]"
        }
    }
}

// add
// Syntax: add [ a | b ] -> c
// Semantic: c := a + b
public class AdditionInstruction: InstructionProtocol {
    public init(augend: Argument<Pseudoregister>, addend: Argument<Pseudoregister>, sum: Result<Pseudoregister>) {
        self.augend = augend
        self.addend = addend
        self.sum = sum
    }

    private(set) public var augend: Argument<Pseudoregister>
    private(set) public var addend: Argument<Pseudoregister>
    private(set) public var sum: Result<Pseudoregister>

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
        closure(&self.augend)
        closure(&self.addend)
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
        closure(&self.sum)
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return true // flags
    }

    public var description: String {
        return "addl [ \(self.augend) | \(self.addend) ] -> \(self.sum)"
    }
}

// sub
// Syntax: sub [ a | b ] -> c
// Semantic: c := a - b
public class SubtractionInstruction: InstructionProtocol {
    public init(minuend: Argument<Pseudoregister>, subtrahend: Argument<Pseudoregister>, difference: Result<Pseudoregister>) {
        self.minuend = minuend
        self.subtrahend = subtrahend
        self.difference = difference
    }

    private(set) public var minuend: Argument<Pseudoregister>
    private(set) public var subtrahend: Argument<Pseudoregister>
    private(set) public var difference: Result<Pseudoregister>

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
        closure(&self.minuend)
        closure(&self.subtrahend)
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
        closure(&self.difference)
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return true // flags
    }

    public var description: String {
        return "subl [ \(self.minuend) | \(self.subtrahend) ] -> \(self.difference)"
    }
}

// imul
// Syntax: mul [ a | b ] -> c
// Semantic: c := a * b
public class MultiplicationInstruction: InstructionProtocol {
    public init(multiplicand: Argument<Pseudoregister>, multiplier: Argument<Pseudoregister>, product: Result<Pseudoregister>) {
        self.multiplicand = multiplicand
        self.multiplier = multiplier
        self.product = product
    }

    private(set) public var multiplicand: Argument<Pseudoregister>
    private(set) public var multiplier: Argument<Pseudoregister>
    private(set) public var product: Result<Pseudoregister>

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
        closure(&self.multiplicand)
        closure(&self.multiplier)
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
        closure(&self.product)
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return true // flags
    }

    public var description: String {
        return "mull [ \(self.multiplicand) | \(self.multiplier) ] -> \(self.product)"
    }
}

// idiv
// Syntax: div [ a | b ] -> [ c | d ]
// Semantic: c := a / b
// Semantic: d := a % b
public class DivisionInstruction: InstructionProtocol {
    public init(dividend: Argument<Pseudoregister>, divisor: Argument<Pseudoregister>, quotient: Result<Pseudoregister>, remainder: Result<Pseudoregister>) {
        self.dividend = dividend
        self.divisor = divisor
        self.quotient = quotient
        self.remainder = remainder
    }

    private(set) public var dividend: Argument<Pseudoregister>
    private(set) public var divisor: Argument<Pseudoregister>
    private(set) public var quotient: Result<Pseudoregister>
    private(set) public var remainder: Result<Pseudoregister>

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
        closure(&self.dividend)
        closure(&self.divisor)
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
        closure(&self.quotient)
        closure(&self.remainder)
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return true // flags
    }

    public var description: String {
        return "divl [ \(self.dividend) | \(self.divisor) ] -> [ \(self.quotient) | \(self.remainder) ]"
    }
}



// neg + b/w/l/q
// twos complement
// Syntax: neg a
// Semantic: a := -a
// width is assumed to be 32bit
public class NumericNegationInstruction: InstructionProtocol {
    public init(source: Argument<Pseudoregister>, target: Result<Pseudoregister>) {
        self.source = source
        self.target = target
    }

    private(set) public var source: Argument<Pseudoregister>
    private(set) public var target: Result<Pseudoregister>

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
        closure(&self.source)
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
        closure(&self.target)
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return true // flags
    }

    public var description: String {
        return "negl \(self.source) -> \(self.target)"
    }
}

// not, notb, notw, notl, notq
// not works with all register sizes
// not does not work with memory (ambiguous)
// ones complement
// width is assumbed to be 8bit
// Syntax: not -> a
// Semantic: a := !a
public class LogicalNegationInstruction: InstructionProtocol {
    public init(source: Argument<Pseudoregister>, target: Result<Pseudoregister>) {
        self.source = source
        self.target = target
    }

    private(set) public var source: Argument<Pseudoregister>
    private(set) public var target: Result<Pseudoregister>

    public func apply(_ closure: (inout Argument<Pseudoregister>) -> Void) {
        closure(&self.source)
    }

    public func apply(_ closure: (inout Result<Pseudoregister>) -> Void) {
        closure(&self.target)
    }

    public var hasEffectsOtherThanWritingPseudoregisters: Bool {
        return false // no flags
    }

    public var description: String {
        return "notb \(self.source) -> \(self.target)"
    }
}
