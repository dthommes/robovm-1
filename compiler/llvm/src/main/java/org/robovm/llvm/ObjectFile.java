/*
 * Copyright (C) 2013 RoboVM AB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
package org.robovm.llvm;

import org.robovm.llvm.binding.IntOut;
import org.robovm.llvm.binding.LLVM;
import org.robovm.llvm.binding.LongArray;
import org.robovm.llvm.binding.LongArrayOut;
import org.robovm.llvm.binding.MemoryBufferRefOut;
import org.robovm.llvm.binding.ObjectFileRef;
import org.robovm.llvm.binding.StringOut;
import org.robovm.llvm.binding.SymbolFlags;
import org.robovm.llvm.binding.SymbolIteratorRef;
import org.robovm.llvm.debuginfo.DwarfDebugMethodInfo;
import org.robovm.llvm.debuginfo.DwarfDebugObjectFileInfo;
import org.robovm.llvm.debuginfo.DwarfDebugVariableInfo;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class ObjectFile implements AutoCloseable {
    private final File file;
    private ObjectFileRef ref;

    private ObjectFile(File file, ObjectFileRef objectFileRef) {
        this.file = file;
        this.ref = objectFileRef;
    }
    
    protected final void checkDisposed() {
        if (ref == null) {
            throw new LlvmException("Already disposed");
        }
    }
    
    protected ObjectFileRef getRef() {
        checkDisposed();
        return ref;
    }

    public boolean isMachO() {
        return LLVM.IsMachOObjectFile(getRef());
    }

    public boolean isCOFF() {
        return LLVM.IsCOFFObjectFile(getRef());
    }

    public List<Symbol> getSymbols() {
        if (isMachO() || isCOFF())
            return getMachOSymbols();
        else
            return getSymbolsElf();
    }

    // returns symbols original way assuming that size information is available
    private List<Symbol> getSymbolsElf() {
        List<Symbol> result = new ArrayList<>();
        SymbolIteratorRef it = LLVM.GetSymbols(getRef());
        while (!LLVM.IsSymbolIteratorAtEnd(getRef(), it)) {
            String name = LLVM.GetSymbolName(it);
            long address = LLVM.GetSymbolAddress(it);
            long size = LLVM.GetSymbolSize(it);
            result.add(new Symbol(name, address, size));
            LLVM.MoveToNextSymbol(it);
        }
        LLVM.DisposeSymbolIterator(it);
        return result;
    }

    /**
     * The version differs from getSymbols in the way that it has to calculate symbols size
     * On MachO symbol size is available only for Common linked ones. So we have to finds
     * out all symbols in section, sort it, and assume symbol size is a gap between two symbols
     * LLVM stopped this way of size evaluation on MachO as there are possible side effects.
     * Ref: http://llvm.org/viewvc/llvm-project?view=revision&revision=238028
     */
    private List<Symbol> getMachOSymbols() {
        List<Symbol> result = new ArrayList<>();
        SymbolIteratorRef it = LLVM.GetSymbols(getRef());
        SectionIterator sectionIt = getSectionIterator();
        Map<String, SectionInfo> unresolved = new HashMap<>();
        while (!LLVM.IsSymbolIteratorAtEnd(getRef(), it)) {
            String name = LLVM.GetSymbolName(it);
            long address = LLVM.GetSymbolAddress(it);
            long flags = LLVM.GetSymbolFlags(it);
            if ((flags & SymbolFlags.SF_Common.swigValue()) != 0) {
                // it is common linkage, can add it directly to result as size is known
                long size = LLVM.GetSymbolSize(it);
                result.add(new Symbol(name, address, size));
            } if ((flags & SymbolFlags.SF_Global.swigValue()) == 0 || (flags & SymbolFlags.SF_Undefined.swigValue()) != 0) {
                // symbol is not global or undefined, don't bother with it and assume its size 0
                result.add(new Symbol(name, address, 0));
            } else {
                // size is not known, add to sections for future resolution
                sectionIt.moveToContainingSection(it);
                String sectionName = sectionIt.getName();
                SectionInfo sectionInfo = unresolved.get(sectionName);
                if (sectionInfo == null) {
                    sectionInfo = new SectionInfo(sectionIt.getAddress(), sectionIt.getSize());
                    unresolved.put(sectionName, sectionInfo);
                }
                sectionInfo.symbols.add(new Symbol(name, address, -1));
            }
            LLVM.MoveToNextSymbol(it);
        }
        LLVM.DisposeSymbolIterator(it);
        sectionIt.dispose();

        // now process unresolved, sort each section and calculate symbol size as a gap between symbols
        Comparator<Symbol> symbolComparator = Symbol.getAddressComparator();
        for (SectionInfo sectionInfo : unresolved.values()) {
            if (sectionInfo.symbols.size() > 1)
                sectionInfo.symbols.sort(symbolComparator);
            // move from first till excluding last
            for (int idx = 0; idx < sectionInfo.symbols.size() - 1; idx++) {
                Symbol raw = sectionInfo.symbols.get(idx);
                Symbol next = sectionInfo.symbols.get(idx + 1);
                result.add(new Symbol(raw.getName(), raw.getAddress(), next.getAddress() - raw.getAddress()));
            }
            // assume last symb last till end of section
            Symbol last = sectionInfo.symbols.get(sectionInfo.symbols.size() - 1);
            long size = (sectionInfo.addr + sectionInfo.size) - last.getAddress();
            result.add(new Symbol(last.getName(), last.getAddress(), size));
        }

        return result;
    }

    public SectionIterator getSectionIterator() {
        return new SectionIterator(this, LLVM.GetSections(getRef()));
    }

    public List<LineInfo> getLineInfos(Symbol symbol) {
        List<LineInfo> result = new ArrayList<>();
        IntOut sizeOut = new IntOut();
        LongArrayOut out = new LongArrayOut();
        LLVM.GetLineInfoForAddressRange(getRef(), symbol.getAddress(), symbol.getSize(), sizeOut, out);
        int size = sizeOut.getValue();
        if (size > 0) {
            LongArray values = out.getValue();
            for (int i = 0; i < size; i++) {
                long address = values.get(i * 3);
                long lineNumber = values.get(i * 3 + 1);
                long columnNumber = values.get(i * 3 + 2);
                result.add(new LineInfo(address, (int) lineNumber, (int) columnNumber));
            }
            values.delete();
        }
        out.delete();
        return result;
    }

    /**
     * Reads DWARF debug information from object file
     * Currently it includes method and variable names
     * @return debug information received from object file or null otherwise
     */
    public DwarfDebugObjectFileInfo getDebugInfo() {
        byte[] bytes = LLVM.DumpDwarfDebugData(getRef());
        if (bytes.length == 0)
            return null;

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // read data
        List<DwarfDebugMethodInfo> methods = new ArrayList<>();
        while (buffer.hasRemaining()) {
            // read method name
            int strLen = buffer.getInt();
            if (strLen == 0) // end of methods
                break;
            // read method name
            String methodName = null;
            try {
                methodName =  new String(buffer.array(), buffer.position(), strLen, "UTF-8");
            } catch (UnsupportedEncodingException ignored) {
            }

            buffer.position(buffer.position() + strLen);
            if (methodName == null) // should not happen
                break;

            List<DwarfDebugVariableInfo> variables = new ArrayList<>();
            // read local variables
            while (buffer.hasRemaining()) {
                strLen = buffer.getInt();
                if (strLen == 0) // end of variable
                    break;

                // read variable name
                String variableName = null;
                try {
                    variableName =  new String(buffer.array(), buffer.position(), strLen, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
                buffer.position(buffer.position() + strLen);
                if (variableName == null) // should not happen
                    break;

                // read variable flags, reg, offset
                byte flags = buffer.get();
                int reg = (256 + buffer.get()) & 0xFF;
                int offset = buffer.getInt();

                // create variable struct
                variables.add(new DwarfDebugVariableInfo(variableName, (flags & 1) == 1, reg, offset));
            }

            methods.add(new DwarfDebugMethodInfo(methodName, variables.toArray(new DwarfDebugVariableInfo[0])));
        }

        return new DwarfDebugObjectFileInfo(null, methods.toArray(new DwarfDebugMethodInfo[0]));
    }

    public synchronized void dispose() {
        LLVM.DisposeObjectFile(getRef());
        ref = null;
    }

    @Override
    public void close() {
        dispose();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((file == null) ? 0 : file.hashCode());
        result = prime * result + ((ref == null) ? 0 : ref.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ObjectFile [file=" + file + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ObjectFile other = (ObjectFile) obj;
        if (file == null) {
            if (other.file != null) {
                return false;
            }
        } else if (!file.equals(other.file)) {
            return false;
        }
        if (ref == null) {
            if (other.ref != null) {
                return false;
            }
        } else if (!ref.equals(other.ref)) {
            return false;
        }
        return true;
    }

    public static ObjectFile load(File file) {
        MemoryBufferRefOut memBufOut = new MemoryBufferRefOut();
        StringOut errorMsgOut = new StringOut();
        LLVM.CreateMemoryBufferWithContentsOfFile(file.getAbsolutePath(), memBufOut, errorMsgOut);
        if (memBufOut.getValue() == null) {
            throw new LlvmException("Failed to create memory buffer from " + file.getAbsolutePath() 
                    + (errorMsgOut.getValue() != null ? ":" + errorMsgOut.getValue() : ""));
        }
        ObjectFileRef ref = LLVM.CreateObjectFile(memBufOut.getValue());
        if (ref == null) {
            throw new LlvmException("Failed to create object file " + file.getAbsolutePath());
        }
        return new ObjectFile(file, ref);
    }

    private static class SectionInfo {
        final long addr;
        final long size;
        final List<Symbol> symbols = new ArrayList<>();

        SectionInfo(long addr, long size) {
            this.addr = addr;
            this.size = size;
        }
    }
}
