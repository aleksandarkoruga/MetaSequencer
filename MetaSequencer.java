
import com.cycling74.max.Atom;
import com.cycling74.max.DataTypes;
import com.cycling74.msp.MSPPerformer;
import com.cycling74.msp.MSPSignal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

public class  MetaSequencer extends MSPPerformer{
	 
	public class ListSequencer
	{
		private Hashtable<Atom,Atom[]> tags=new Hashtable<Atom,Atom[]>();
		private Atom[] current_sequence;
		private Vector<Atom[]> collection;
		private Vector<Atom[]> structure;
		private int collection_idx,clock_type ;
		private int relative_idx;
		private int [] structure_idx; //0 atom 1 vector
		private int[] idx;
		private int seq_outlet;
		private int Eseq_outlet;
		private double clock;
	//	private boolean trigger;
		int subdivision;
		boolean external_subdivision;
		
		
		public ListSequencer(int _seq_outlet,int _E_seq_outlet)
		{
			Eseq_outlet=_E_seq_outlet;
			relative_idx=0;
			seq_outlet=_seq_outlet;
			idx=new int[3];
			Arrays.fill(idx,0);
			clock_type=0;
			clock=0;
			structure_idx=new int[2];
			Arrays.fill(structure_idx, 0);
			collection_idx=0;
			subdivision=16;
			external_subdivision=true;
			collection=new Vector<Atom[]>();
			structure=new Vector<Atom[]>();
			
		}
		public void SetClockType(int _type)
		{
			clock_type=_type;
		}
		public void AddStructureOrder(Atom[] _input)
		{
			structure.addElement(_input);
		}
		
		public void AddSequence(Atom[] _input)
		{
			
			//Atom[] input=GetTag(_input);
		
			collection.addElement(_input);
		}
		public void AddTaggedSequence(Atom[] _input)
		{
			
			Atom[] input=GetTag(_input);
		
			collection.addElement(input);
		}
		private Atom[] GetTag(Atom[] _input)
		{
			
			if(_input[0].isInt())
				return _input;
			
			
		//	 tags.put(Atom.removeFirst(_input), _input);//maybe not working (does remove first get executed)
			//Atom[] first=Atom.removeFirst(_input);	//overdoing it here just in case, debugger is crap
			//post(Atom.toDebugString(_input)+ " " +Atom.toDebugString(first) );

			tags.put(_input[0], Atom.removeFirst(_input));
			
		
			return Atom.removeFirst(_input);
		}
		public void SetClock(float _fclock)
		{
			double _clock=(double) _fclock;
			
			if(_clock-clock<0){relative_idx=0; Step(_clock,true); clock=_clock; return; }
			//TODO!!! clock overkill,  better clock=_fclock*subdivision
			double _fract = _clock%InvSub();
			double fract= clock%InvSub();
			
			if(_fract-fract<0){ Step(_clock,false);clock=_clock;return; }
			clock=_clock;
		
		}
		private void Step(double _clock,boolean _newmeasure)
		{
			
			
			if(collection.isEmpty())return;
			
			if(_newmeasure)
			{
				idx[0]=0;
				idx[1]=0;
								
				SetNextSequence();
				OutputEntireSequence();
			}
			else
			{
				int _idx=idx[0];
				idx[0]=(int)Math.floor(_clock * (double)subdivision)%subdivision;
				if(_idx==idx[0])return;
				idx[1]++;
			}
			idx[2]++;
			idx[2]%=current_sequence.length;
			if(clock_type==2 && idx[2]==0){ SetNextSequence();OutputEntireSequence(); }
			
			ComputeOutput();
			
			
		}
		private void ComputeOutput()
		{
			if(collection.isEmpty())return;
			int _len=current_sequence.length;
			int _idx= idx[clock_type];								//TODO: correct for length
			//_idx= _idx>_len ? _idx%_len:_idx;
			/*Atom[] a=new Atom[5];
			a[0]=Atom.newAtom(idx[clock_type]);
			a[1]=Atom.newAtom("clock: "+clock);
			a[2]=Atom.newAtom("floor(cs%s) "+Math.floor(clock * (double)subdivision)%subdivision);
			a[3]=Atom.newAtom("clock*sub%sub "+(clock * (double)subdivision)%subdivision);
			a[4]=Atom.newAtom("clock*sub "+(clock * (double)subdivision));
			a[4]=Atom.newAtom("length "+_len);
			outlet(2,a);*/
			
			
			
			if((_idx>=_len) && (relative_idx==0 || _idx-relative_idx>=_len)) 
			{
				SetNextSequence();
				OutputEntireSequence(); 
				relative_idx=_idx;
				_idx-=relative_idx;
			}
			if(_idx>=_len)
			_idx-=relative_idx;
			
			
			outlet(seq_outlet, current_sequence[_idx]);
			
			
			
			
		}
		public void OutputEntireSequence()
		{
			outlet(Eseq_outlet,current_sequence );
			
			
		}
		
		private void SetNextSequence()
		{
			if(structure==null || structure.isEmpty())
			{
				collection_idx++;
				collection_idx%=collection.size();
				current_sequence=collection.elementAt(collection_idx);
			}
			else
			{
				
				
				structure_idx[0]++;
				if(structure_idx[0]> structure.elementAt(structure_idx[1]).length)
				{structure_idx[0]=0; 
				structure_idx[1]++;
				structure_idx[1]%=structure.size();
				}
				if(structure.elementAt(structure_idx[1])[structure_idx[0]].isInt())
				{
				   
				   collection_idx= Atom.toInt(structure.elementAt(structure_idx[1]))[structure_idx[0]];
				   current_sequence=collection.elementAt(collection_idx);
				   
				   if(external_subdivision){return;}
				   subdivision= current_sequence.length;
				   return;
				}
				
				
				Atom key= structure.elementAt(structure_idx[1])[structure_idx[0]];
				
				if(!tags.containsKey(key)){return;}
				
				
				current_sequence=tags.get(key);
				if(external_subdivision){return;}
				subdivision= current_sequence.length;
				//post(Atom.toDebugString(value));
				
			//	if(!collection.contains(value)){post("nada");return;}
					//collection_idx=collection.indexOf(value);
				
					
			}
			
			
			//current_sequence=collection.elementAt(collection_idx);
			
			
			
		}
		
		
		public void SetSubdivision(int _subdivision)
		{
			if(_subdivision<=0){ external_subdivision=false;return; }
			external_subdivision=true;
			subdivision=_subdivision;
			
		}
		public void ReplaceSequence(Atom _index,Atom[] _sequence)
		{
			if(_index.isInt()){collection.set(_index.toInt(), _sequence);return;}
			tags.replace(_index, _sequence);
		
		}
		public void InsertSequence(Atom _index, Atom[] _sequence)
		{
			
			if(_index.isInt()){collection.insertElementAt( _sequence,_index.toInt());return;}
			tags.put(_index, _sequence);
		}
		public void ClearCollection()
		{
			collection.clear();
		}
		public void DeleteSequence(Atom[] _index)
		{
			int _idx=0;
			if(_index[0].isInt())
			{	
				 _idx=_index[0].toInt();
				if(_idx>=collection.size() || _idx<0){return;}
			
			collection.removeElementAt(_idx);
			return;
			}
			tags.remove(_index);
			
		}		
		public void DeleteStructure()
		{
			structure.clear();
		}
		private double InvSub()
		{return (1.0/(double)subdivision);}
		
	}
	
	/////////////////	/////////////////	/////////////////	/////////////////	/////////////////	/////////////////
	
	
	
	 int vec_size;
	 double sr;
	 float external_clock;
	 ListSequencer sequencer=new ListSequencer(1,2);
	 
	public MetaSequencer(){				// the last two inlets can be removed
		declareInlets(new int[]{SIGNAL,DataTypes.ANYTHING,DataTypes.LIST});
		declareOutlets(new int[]{SIGNAL,DataTypes.LIST,DataTypes.LIST});
		
	//	_f1 = getPerformMethod("f1");
	}
	public void clocktype(Atom[] _type)
	{
		if(_type[0].isInt())
		{switch(_type[0].toInt())
			{
				case 0:sequencer.SetClockType(0); break;
				case 1:sequencer.SetClockType(1);break;
				case 2:sequencer.SetClockType(2);break;
				default: return;
			}
		}
	}
	public void replacesequence(Atom[] _index)
	{
		sequencer.ReplaceSequence(_index[0], Atom.removeFirst(_index));
		
	}
	public void insertsequence(Atom[] _index)
	{
		sequencer.InsertSequence(_index[0], Atom.removeFirst(_index));
		
	}
	
	public void subdivision(Atom[] _type)
	{
		if(_type[0].isInt())
		{
			sequencer.SetSubdivision(_type[0].toInt());
		}
	}
	public void deletesequence(Atom[] _index)
	{
		sequencer.DeleteSequence(_index);
		
	}
	
	public void clearstructure(Atom[] _input)
	{
		sequencer.DeleteStructure();
	}
	public void clearcollection(Atom[] _input)
	{
		sequencer.ClearCollection();
	}
	public void taggedsequence(Atom[] _input)
	{
		sequencer.AddTaggedSequence(_input);	
	}
	
	public void sequence(Atom[] _input)
	{
		sequencer.AddSequence(_input);	
	}
	public void structure(Atom[] _input)
	{
		sequencer.AddStructureOrder(_input);
	}
	/*public void list(Atom[] _input)
	{
		int _inlet=getInlet();
		if(_inlet==1)
		{
				sequence(_input);
		}
		if(_inlet==2)
		{
			
			    structure(_input);
		}
		
		
		
		
	}*/
	
	
	public void dspsetup(MSPSignal[] in, MSPSignal[] out)
	{
		vec_size =  in[0].vec.length;
		sr= in[0].sr ;
		
		
	}
	public void perform(MSPSignal[] in, MSPSignal[] out) {
		if(in[0].connected)
		for(int q=0;q<vec_size;q++)
		{
			
		     sequencer.SetClock( in[0].vec[q]); 
		      
		}
		
		
		

		
		//out[0].vec[i]=(float);
		}
	}
		
	
	
	
	
	
	
	
	
	
	

